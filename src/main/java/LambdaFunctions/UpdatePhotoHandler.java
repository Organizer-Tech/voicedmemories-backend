package LambdaFunctions;

import AwsServices.Database;
import AwsServices.FileStorage;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;

public class UpdatePhotoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        Database dynamoDb = Database.getInstance();
        FileStorage s3 = FileStorage.getInstance();
        Map<String, String> pathParameters = input.getPathParameters();
        JsonObject body = JsonParser.parseString(input.getBody()).getAsJsonObject();

        String email = pathParameters.get("email");
        String photoId = pathParameters.get("id");
        String photoTitle = null;
        String album = null;

        if (body.has("album")) {
            album = body.get("album").getAsString();
        } else {
            album = pathParameters.get("album").replace("%20", " ");
        }
        if (body.has("photo title")) {
            photoTitle = body.get("photo title").getAsString();
        }

        String photoKey = null;
        String audioKey = null;
        int position = -1;

        try {
            if (body.has("photo")) {
                if (!body.has("photo type")) {
                    throw new IllegalArgumentException();
                }
                System.out.println("---before upload photo");
                photoKey = s3.uploadFile(body.get("photo").getAsString(), body.get("photo type").getAsString(), email, album, photoId);
            }
            if (body.has("audio")) {
                if (!body.has("audio type")) {
                    throw new IllegalArgumentException();
                }
                System.out.println("--before audio upload");
                audioKey = s3.uploadFile(body.get("audio").getAsString(), body.get("audio type").getAsString(), email, album, photoId);
            }
            if (!album.equals(pathParameters.get("album").replace("%20", " "))) {
                Map<String, String> oldFileKeys = dynamoDb.getFileKeys(pathParameters);

                if (photoKey == null) {
                    photoKey = s3.moveFile(oldFileKeys.get("photoKey"), email, album, photoId);
                }

                if (audioKey == null && oldFileKeys.containsKey("audioKey")) {
                    audioKey = s3.moveFile(oldFileKeys.get("audioKey"), email, album, photoId);
                }
            }
            if (body.has("position")) {
                position = body.get("position").getAsInt();
            }
        } catch (IllegalArgumentException | SdkClientException ex) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.UNSUPPORTED_MEDIA_TYPE).withBody("The file could not be uploaded. Please ensure the file is base64 encoded and has a valid extension type.");
        }

        String updatedProperties;
        try {
            updatedProperties = dynamoDb.updateEntry(email, album, photoTitle, photoKey, audioKey, photoId, position);
        } catch (AmazonDynamoDBException ade) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.NOT_FOUND).withBody(ade.getMessage());
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.OK).withBody(updatedProperties);
    }
}
