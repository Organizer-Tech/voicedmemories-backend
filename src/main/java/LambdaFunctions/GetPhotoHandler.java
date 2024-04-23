package LambdaFunctions;

import AwsServices.Database;
import AwsServices.FileStorage;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Map;

/**
 * Lambda function that retrieves the encoded image and audio files for a requested photo.
 */
public class GetPhotoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        FileStorage s3 = FileStorage.getInstance();
        Database dynamoDb = Database.getInstance();
        Map<String, String> parameters = input.getPathParameters();
        String encodedPhoto = "";
        String encodedAudio = "";

        Map<String, String> fileKeys = dynamoDb.getFileKeys(parameters);
        try {
            encodedPhoto = s3.downloadFile(fileKeys.get("photoKey"));
            encodedAudio = s3.downloadFile(fileKeys.get("audioKey"));
        } catch (IOException io) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.INTERNAL_SERVER_ERROR).withBody(io.getMessage());
        }

        if (encodedPhoto.isEmpty()) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.NOT_FOUND).withBody("The requested photo could not be found.");
        }


        JsonObject result = new JsonObject();
        result.addProperty("position", fileKeys.get("position"));
        result.addProperty("photo", encodedPhoto);

        if (!encodedAudio.isEmpty()) {
            result.addProperty("audio", encodedAudio);
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.OK).withBody(result.toString());
    }
}
