package LambdaFunctions;

import AwsServices.Database;
import AwsServices.FileStorage;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.List;
import java.util.Map;

public class DeleteAllPhotosHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        FileStorage s3 = FileStorage.getInstance();
        Database dynamoDB = Database.getInstance();
        Map<String, String> pathParameters = input.getPathParameters();

        if (!pathParameters.containsKey("email") || pathParameters.get("email") == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.BAD_REQUEST);
        }

        String email = pathParameters.get("email");
        List<Map<String, String>> allFileKeys = dynamoDB.getAllFileKeys(email);

        dynamoDB.deleteAllPhotos(email, allFileKeys);

        int failCount = 0;
        for (Map<String, String> fileKey : allFileKeys) {
            try {
                if (fileKey.containsKey("photoKey")) {
                    s3.deleteFile(fileKey.get("photoKey"));
                }

                if (fileKey.containsKey("audioKey")) {
                    s3.deleteFile(fileKey.get("audioKey"));
                }
            } catch (SdkClientException sce) {
                failCount++;
            }
        }

        if (failCount > 0) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(StatusCodes.NOT_FOUND)
                    .withBody(failCount + " files could not be deleted.");
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.NO_CONTENT);
    }
}
