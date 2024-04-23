package AwsServices;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Authenticator {
    private static Authenticator instance;
    private final AWSCognitoIdentityProvider cognito;

    private Authenticator() {
        cognito = AWSCognitoIdentityProviderClientBuilder.standard().withRegion(Regions.US_WEST_2).build();
    }

    public static Authenticator getInstance() {
        if (instance == null) {
            instance = new Authenticator();
        }

        return instance;
    }

    public String getAuthenticatedEmail(String accessToken) {
        GetUserRequest userRequest = new GetUserRequest().withAccessToken(accessToken);
        GetUserResult userResult = cognito.getUser(userRequest);

        String authenticatedEmail = "";
        List<AttributeType> attributes = userResult.getUserAttributes();
        for (AttributeType attributeType : attributes) {
            if (attributeType.getName().equals("email")) {
                authenticatedEmail = attributeType.getValue();
            }
        }

        return authenticatedEmail;
    }

    public JsonObject getTokens() {
        String clientId = "78m649jtthpnucb7vgal7cge7i";
        String userPoolId = "us-west-2_Y90Sv1Elj";
        Map<String, String> authParameters = new HashMap<>();

        authParameters.put("USERNAME", "kelstondosh@gmail.com");
        authParameters.put("PASSWORD", "TestPass1!");

        AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest()
                .withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .withClientId(clientId)
                .withUserPoolId(userPoolId)
                .withAuthParameters(authParameters);

        AdminInitiateAuthResult authResult = cognito.adminInitiateAuth(authRequest);
        AuthenticationResultType resultType = authResult.getAuthenticationResult();

        JsonObject tokens = new JsonObject();
        tokens.addProperty("Access Token", resultType.getAccessToken());
        tokens.addProperty("Id Token", resultType.getIdToken());

        return tokens;
    }
}
