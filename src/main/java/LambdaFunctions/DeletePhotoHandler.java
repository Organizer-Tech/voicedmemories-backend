package LambdaFunctions;

import AwsServices.Database;
import AwsServices.FileStorage;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

/**
 * Lambda function to handle delete requests of individual photos
 *
 */
public class DeletePhotoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        FileStorage s3 = FileStorage.getInstance();
        Database dynamoDB = Database.getInstance();
        Map<String, String> pathParameters = input.getPathParameters();

        if (!pathParameters.containsKey("email") || pathParameters.get("email") == null
                || !pathParameters.containsKey("id") || pathParameters.get("id") == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.BAD_REQUEST);
        }

        String email = pathParameters.get("email");
        String photoID = pathParameters.get("id");
        Map<String, String> fileKeys = dynamoDB.getFileKeys(pathParameters);

        dynamoDB.deletePhoto(email, photoID);

        try {
            if (fileKeys.containsKey("photoKey")) {
                s3.deleteFile(fileKeys.get("photoKey"));
            }

            if (fileKeys.containsKey("audioKey")) {
                s3.deleteFile(fileKeys.get("audioKey"));
            }
        } catch (SdkClientException sce) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.NOT_FOUND);
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.NO_CONTENT);
    }
}
