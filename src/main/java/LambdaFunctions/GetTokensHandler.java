package LambdaFunctions;

import AwsServices.Authenticator;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class GetTokensHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Authenticator cognito = Authenticator.getInstance();

        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.OK).withBody(cognito.getTokens().toString());
    }
}
