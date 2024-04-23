package LambdaFunctions;

import AwsServices.Database;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonArray;

import java.util.HashMap;
import java.util.Map;

public class GetSignedLinksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Database database = Database.getInstance();
        String id = input.getPathParameters().get("shareId");
        String shareUrl = "http://my.voicedmemories.ca/shared?id=" + id;
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Access-Control-Allow-Origin", "*");

        JsonArray sharedLinks = database.getSharedLinks(shareUrl);

        if (sharedLinks == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.NOT_FOUND).withHeaders(responseHeaders);
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(StatusCodes.OK).withHeaders(responseHeaders).withBody(sharedLinks.toString());
    }
}
