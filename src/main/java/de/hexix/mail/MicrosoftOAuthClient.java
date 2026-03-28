package de.hexix.mail;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "microsoft-oauth-api")
public interface MicrosoftOAuthClient {

    @POST
    @Path("/{tenant}/oauth2/v2.0/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    TokenResponse getToken(
            @PathParam("tenant") String tenant,
            @FormParam("client_id") String clientId,
            @FormParam("scope") String scope,
            @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("grant_type") String grantType,
            @FormParam("client_secret") String clientSecret
    );

    @POST
    @Path("/{tenant}/oauth2/v2.0/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    TokenResponse refreshToken(
            @PathParam("tenant") String tenant,
            @FormParam("client_id") String clientId,
            @FormParam("scope") String scope,
            @FormParam("refresh_token") String refreshToken,
            @FormParam("grant_type") String grantType,
            @FormParam("client_secret") String clientSecret
    );

    class TokenResponse {
        public String token_type;
        public String scope;
        public int expires_in;
        public int ext_expires_in;
        public String access_token;
        public String refresh_token;
        public String id_token;
    }
}
