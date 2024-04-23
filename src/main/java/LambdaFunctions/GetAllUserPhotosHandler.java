package LambdaFunctions;

import AwsServices.Database;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Lambda function to get all of a user's photo urls from the database.
 */
public class GetAllUserPhotosHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Database dynamoDb = Database.getInstance();
        Map<String, String> pathParameters = input.getPathParameters();
        String email = pathParameters.get("email");
        JsonObject photoUrls = dynamoDb.getPhotoUrls(email);

        if (photoUrls.isEmpty()) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.NOT_FOUND).withBody("The user could not be found");
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.OK).withBody(photoUrls.toString());
    }
}
