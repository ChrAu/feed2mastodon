package de.hexix.mail;

import de.hexix.mail.model.MailboxAccount;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Path("/api/oauth")
public class MailOAuthResource {

    @Inject
    @RestClient
    MicrosoftOAuthClient microsoftOAuthClient;

    @Inject
    MailboxAccountService mailboxAccountService;

    @ConfigProperty(name = "microsoft.oauth.client.id")
    String clientId;

    @ConfigProperty(name = "microsoft.oauth.client.secret")
    String clientSecret;

    @ConfigProperty(name = "microsoft.oauth.redirect.uri")
    String redirectUri;
    
    @ConfigProperty(name = "microsoft.oauth.tenant.id")
    String tenantId;

    @GET
    @Path("/login")
    @Transactional
    public Response login(@QueryParam("email") String email) {
        if (email == null || email.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email query parameter is required.").build();
        }

        MailboxAccount account = mailboxAccountService.getMailboxAccountByEmail(email);
        if (account == null) {
            // Create a new account if it doesn't exist
            account = new MailboxAccount(email, "outlook.office365.com", 993, email, "imaps");
            account.setAuthenticationType("OAUTH");
            mailboxAccountService.addMailboxAccount(account); // Fixed method name
        }

        String scope = "https://outlook.office.com/IMAP.AccessAsUser.All offline_access";
        
        // URL-encode the parameters to avoid URI syntax exceptions
        String authorizationUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/authorize" +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_mode=query" +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(email, StandardCharsets.UTF_8);

        return Response.seeOther(URI.create(authorizationUrl)).build();
    }

    @GET
    @Path("/callback")
    @Transactional
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String email) {
        if (code == null || email == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Authorization code or state is missing.").build();
        }

        MailboxAccount account = mailboxAccountService.getMailboxAccountByEmail(email);
        if (account == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Mailbox account not found for email: " + email).build();
        }

        String scope = "https://outlook.office.com/IMAP.AccessAsUser.All offline_access";
        MicrosoftOAuthClient.TokenResponse tokenResponse = microsoftOAuthClient.getToken(
                tenantId,
                clientId,
                scope,
                code,
                redirectUri,
                "authorization_code",
                clientSecret
        );

        account.setAccessToken(tokenResponse.access_token);
        account.setRefreshToken(tokenResponse.refresh_token);
        account.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.expires_in));
        
        mailboxAccountService.updateMailboxAccount(account);

        return Response.ok("OAuth2 token received and stored for " + email).build();
    }
}
