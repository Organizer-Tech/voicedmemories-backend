package LambdaFunctions;

import AwsServices.Authenticator;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

public class RequestRouter implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> requestHeaders = input.getHeaders();
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Access-Control-Allow-Origin", "*");

        Authenticator cognito = Authenticator.getInstance();
        String accessToken = requestHeaders.get("access-token");
        String email = input.getPathParameters().get("email");
        String authenticatedEmail = cognito.getAuthenticatedEmail(accessToken);

        if (!email.equals(authenticatedEmail)) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(StatusCodes.UNAUTHORIZED)
                    .withBody("Access Denied")
                    .withHeaders(responseHeaders);
        }

        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> function = RequestHandlerFactory.getRequestHandler(input);

        // Check that the function name is valid
        if (function == null) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(StatusCodes.INTERNAL_SERVER_ERROR)
                    .withBody("API Gateway or function list configured incorrectly. Router received request it was not configured for.\nMethod: " + input.getHttpMethod() + "\nPath: " + input.getPath())
                    .withHeaders(responseHeaders);
        }

        // Call the function and return the result
        return function.handleRequest(input, context).withHeaders(responseHeaders);
    }
}
