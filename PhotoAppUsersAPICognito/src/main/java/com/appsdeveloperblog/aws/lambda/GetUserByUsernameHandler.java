package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

public class GetUserByUsernameHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    private final String poolId;

    public GetUserByUsernameHandler(CognitoUserService cognitoUserService,
                             String appClientId,
                             String appClientSecret,
                                    String poolId) {
        this.cognitoUserService = cognitoUserService;
        this.appClientId = appClientId;
        this.appClientSecret = appClientSecret;
        this.poolId=poolId;
    }

    public GetUserByUsernameHandler() {
        this.cognitoUserService  = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
        this.poolId = Utils.decryptKey("MY_COGNITO_POOL_ID");
    }
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent=new APIGatewayProxyResponseEvent();
        Map<String, String> headers=new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        apiGatewayProxyResponseEvent.setHeaders(headers);

        JsonObject userDetails = null;
        String inputBody = input.getBody();

        try {
            userDetails = JsonParser.parseString(inputBody).getAsJsonObject();
            String userName=userDetails.get("email").getAsString();
            JsonObject user = cognitoUserService.getUserByUsername(userName, poolId);

            apiGatewayProxyResponseEvent.withStatusCode(200);
            apiGatewayProxyResponseEvent.withBody(new Gson().toJson(user,JsonObject.class));
        } catch (AwsServiceException ex) {
            ErrorResponse errorResponse = new ErrorResponse(ex.awsErrorDetails().errorMessage());
            String errorResponseJsonString = new Gson().toJson(errorResponse, ErrorResponse.class);
            apiGatewayProxyResponseEvent.withBody(errorResponseJsonString);
            apiGatewayProxyResponseEvent.withStatusCode(ex.awsErrorDetails().sdkHttpResponse().statusCode());
        } catch (Exception ex) {
            ErrorResponse errorResponse = new ErrorResponse(ex.getMessage());
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            apiGatewayProxyResponseEvent.withBody(errorResponseJsonString);
            apiGatewayProxyResponseEvent.withStatusCode(500);
        }
        return apiGatewayProxyResponseEvent;
    }
}
