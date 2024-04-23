package LambdaFunctions;

import AwsServices.Database;
import AwsServices.FileStorage;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.UUID;

/**
 * Lambda function to handle post requests
 */
public class UploadPhotoHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Database dynamoDb = Database.getInstance();
        FileStorage s3 = FileStorage.getInstance();
        Map<String, String> parameters = input.getPathParameters();
        JsonObject body = JsonParser.parseString(input.getBody()).getAsJsonObject();
        String uuid = UUID.randomUUID().toString();
        String photoKey = null;
        String audioKey = null;
        int position;

        if (!validateInput(parameters, body)) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.BAD_REQUEST).withBody("Input is invalid. Please ensure the request body and parameters are correct.");
        }

        try {
            photoKey = s3.uploadFile(body.get("photo").getAsString(), body.get("photo type").getAsString(), parameters.get("email"), parameters.get("album"), uuid);

            if (body.has("audio")) {
                audioKey = s3.uploadFile(body.get("audio").getAsString(), body.get("audio type").getAsString(), parameters.get("email"), parameters.get("album"), uuid);
            }
        } catch (IllegalArgumentException | SdkClientException ex) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.UNSUPPORTED_MEDIA_TYPE).withBody("The file could not be uploaded. Please ensure the file is base64 encoded and has a valid extension type.");
        }

        position = body.get("position").getAsInt();
        String url = dynamoDb.createEntry(body, parameters, photoKey, audioKey, position, uuid);

        JsonObject result = new JsonObject();
        result.addProperty("Location", url);

        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.CREATED).withBody(result.toString());
    }

    private boolean validateInput(Map<String, String> parameters, JsonObject body) {
        if (!parameters.containsKey("email") || !parameters.containsKey("album")) {
            return false;
        }

        if (!body.has("photo title") || !body.has("photo type") || !body.has("photo") || !body.has("position")) {
            return false;
        }

        if (body.has("audio")) {
            return body.has("audio type");
        }

        return true;
    }
}
