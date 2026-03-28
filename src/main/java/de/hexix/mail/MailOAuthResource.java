package de.hexix.mail;

import de.hexix.mail.model.MailboxAccount;
import de.hexix.mail.oauth.OAuthProvider;
import de.hexix.mail.oauth.OAuthTokenService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.logging.Logger;

@Path("/api/oauth")
public class MailOAuthResource {

    private static final Logger LOG = Logger.getLogger(MailOAuthResource.class.getName());

    @Inject
    MailboxAccountService mailboxAccountService;

    @Inject
    OAuthTokenService oauthTokenService;

    @GET
    @Path("/login")
    @Transactional
    public Response login(@QueryParam("email") String email, @QueryParam("provider") String providerId) {
        if (email == null || email.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email query parameter is required.").build();
        }

        OAuthProvider provider = null;

        // 1. Versuche, den Provider über die explizite ID zu finden
        if (providerId != null && !providerId.trim().isEmpty()) {
            provider = oauthTokenService.getProviderById(providerId);
        } else {
            // 2. Wenn keine ID da ist, versuche es über die E-Mail-Domain
            provider = oauthTokenService.getProviderByEmail(email);
        }

        if (provider == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Could not determine a suitable OAuth provider for the given email or provider ID.").build();
        }

        MailboxAccount account = mailboxAccountService.getMailboxAccountByEmail(email);
        if (account == null) {
            // Create a new account using provider defaults if it doesn't exist
            account = provider.createNewAccount(email);
            mailboxAccountService.addMailboxAccount(account);
        }

        String authorizationUrl = provider.getAuthorizationUrl(email);
        return Response.seeOther(URI.create(authorizationUrl)).build();
    }

    @GET
    @Path("/callback")
    @Transactional
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String email,
                             @QueryParam("error") String error, @QueryParam("error_description") String errorDescription) {
        LOG.info("Received OAuth callback. Code: " + code + ", State (email): " + email +
                 ", Error: " + error + ", Error Description: " + errorDescription);

        if (error != null) {
            LOG.warning("OAuth callback received an error: " + error + " - " + errorDescription);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("OAuth error: " + error + (errorDescription != null ? " (" + errorDescription + ")" : "")).build();
        }

        if (code == null || email == null) {
            LOG.warning("Authorization code or state is missing in callback. Code: " + code + ", State: " + email);
            return Response.status(Response.Status.BAD_REQUEST).entity("Authorization code or state is missing.").build();
        }

        MailboxAccount account = mailboxAccountService.getMailboxAccountByEmail(email);
        if (account == null) {
            LOG.warning("Mailbox account not found for email: " + email + " during OAuth callback.");
            return Response.status(Response.Status.NOT_FOUND).entity("Mailbox account not found for email: " + email).build();
        }

        // Find the right provider based on the account configuration
        OAuthProvider provider = oauthTokenService.getProviderForAccount(account);
        if (provider == null) {
            LOG.severe("No suitable OAuthProvider found for account: " + email + " during OAuth callback.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("No suitable OAuthProvider found for account: " + email).build();
        }

        try {
            provider.processCallback(account, code);
            mailboxAccountService.updateMailboxAccount(account);
            LOG.info("OAuth2 token successfully received and stored for " + email);
            return Response.ok("OAuth2 token successfully received and stored for " + email).build();
        } catch (Exception e) {
            LOG.severe("Error during OAuth callback for " + email + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing callback: " + e.getMessage()).build();
        }
    }
}
