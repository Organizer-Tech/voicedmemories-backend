package LambdaFunctions;

import AwsServices.Database;
import AwsServices.FileStorage;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GetShareableLinkHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> parameters = input.getPathParameters();

        System.out.println("---Start---");

        Database database = Database.getInstance();
        FileStorage fileStorage = FileStorage.getInstance();
        String email = parameters.get("email");
        String album = parameters.get("album").replace("%20", " ");
        String sharedUrl = generateSharedUrl();

        List<Map<String, String>> albumPhotos = database.getAlbumPhotos(email, album);
        for (Map<String, String> photo : albumPhotos) {
            String photoKey = photo.get("photo key");
            String audioKey = photo.get("audio key");

            String title = photo.get("photo title");
            String photoUrl = fileStorage.createPresignedUrl(photoKey);
            String audioUrl = fileStorage.createPresignedUrl(audioKey);

            database.createShareEntry(sharedUrl, title, photoUrl, audioUrl);
        }

        JsonObject response = new JsonObject();
        response.addProperty("Shared Url", sharedUrl);
        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.OK).withBody(response.toString());
    }

    private String generateSharedUrl() {
        String baseUrl = "http://my.voicedmemories.ca/shared?id=";
        String uuid = UUID.randomUUID().toString();

        return baseUrl + uuid;
    }
}
