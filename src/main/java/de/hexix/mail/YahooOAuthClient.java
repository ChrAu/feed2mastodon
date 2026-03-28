package de.hexix.mail;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "yahoo-oauth-api")
public interface YahooOAuthClient {

    @POST
    @Path("/oauth2/get_token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    TokenResponse getToken(
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("code") String code,
            @FormParam("grant_type") String grantType,
            @FormParam("scope") String scope // Yahoo might require scope even for token exchange
    );

    @POST
    @Path("/oauth2/get_token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    TokenResponse refreshToken(
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            @FormParam("refresh_token") String refreshToken,
            @FormParam("grant_type") String grantType,
            @FormParam("scope") String scope // Yahoo might require scope even for token refresh
    );

    class TokenResponse {
        public String token_type;
        public String access_token;
        public int expires_in;
        public String refresh_token;
        // Yahoo might return other fields, but these are the most common and necessary
    }
}
