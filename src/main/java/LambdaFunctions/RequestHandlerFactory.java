package LambdaFunctions;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public class RequestHandlerFactory {
    public static RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getRequestHandler(APIGatewayProxyRequestEvent input) {
        String method = input.getHttpMethod();
        Map<String, String> pathParams = input.getPathParameters();

        switch (method) {
            case "GET":
                if (pathParams.containsKey("id")) {
                    return new GetPhotoHandler();
                } else if (pathParams.containsKey("album")) {
                    return new GetShareableLinkHandler();
                } else {
                    return new GetAllUserPhotosHandler();
                }
            case "POST":
                return new UploadPhotoHandler();
            case "PUT":
                return new UpdatePhotoHandler();
            case "DELETE":
                if (pathParams.containsKey("id")) {
                    return new DeletePhotoHandler();
                } else {
                    return new DeleteAllPhotosHandler();
                }
            default:
                return null;
        }
    }
}
