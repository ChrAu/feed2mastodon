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

    private static String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        // Replace line breaks and carriage returns to prevent log injection via multi-line entries
        return input.replace('\n', ' ').replace('\r', ' ');
    }

    private static String escapeForHtml(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

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
        String safeCode = sanitizeForLog(code);
        String safeEmail = sanitizeForLog(email);
        String safeError = sanitizeForLog(error);
        String safeErrorDescription = sanitizeForLog(errorDescription);

        LOG.info("Received OAuth callback. Code: " + safeCode + ", State (email): " + safeEmail +
                 ", Error: " + safeError + ", Error Description: " + safeErrorDescription);

        if (error != null) {
            LOG.warning("OAuth callback received an error: " + safeError + " - " + safeErrorDescription);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("OAuth error returned by provider.").build();
        }

        if (code == null || email == null) {
            LOG.warning("Authorization code or state is missing in callback. Code: " + safeCode + ", State: " + safeEmail);
            return Response.status(Response.Status.BAD_REQUEST).entity("Authorization code or state is missing.").build();
        }

        MailboxAccount account = mailboxAccountService.getMailboxAccountByEmail(email);
        if (account == null) {
            LOG.warning("Mailbox account not found for email: " + safeEmail + " during OAuth callback.");
            return Response.status(Response.Status.NOT_FOUND).entity("Mailbox account not found.").build();
        }

        // Find the right provider based on the account configuration
        OAuthProvider provider = oauthTokenService.getProviderForAccount(account);
        if (provider == null) {
            LOG.severe("No suitable OAuthProvider found for account: " + safeEmail + " during OAuth callback.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("No suitable OAuthProvider found for account.").build();
        }

        try {
            provider.processCallback(account, code);
            mailboxAccountService.updateMailboxAccount(account);
            LOG.info("OAuth2 token successfully received and stored for " + safeEmail);
            return Response.ok("OAuth2 token successfully received and stored.").build();
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.SEVERE, "Error during OAuth callback for " + safeEmail, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing callback.").build();
        }
    }
}
