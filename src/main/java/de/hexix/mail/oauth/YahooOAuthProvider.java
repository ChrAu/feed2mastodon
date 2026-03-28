package de.hexix.mail.oauth;

import de.hexix.mail.YahooOAuthClient; // Assuming this will be created
import de.hexix.mail.model.MailboxAccount;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@ApplicationScoped
public class YahooOAuthProvider implements OAuthProvider {

    private static final Logger LOG = Logger.getLogger(YahooOAuthProvider.class.getName());

    @Inject
    @RestClient
    YahooOAuthClient yahooOAuthClient; // Assuming this will be created

    @ConfigProperty(name = "yahoo.oauth.client.id")
    String clientId;

    @ConfigProperty(name = "yahoo.oauth.client.secret")
    String clientSecret;

    // Yahoo typically doesn't have a tenant ID like Microsoft
    // @ConfigProperty(name = "yahoo.oauth.tenant.id")
    // String tenantId;

    @ConfigProperty(name = "yahoo.oauth.redirect.uri")
    String redirectUri;

    @Override
    public String getProviderId() {
        return "yahoo";
    }

    @Override
    public boolean supports(MailboxAccount account) {
        return account.getHost() != null && account.getHost().contains("mail.yahoo.com");
    }

    @Override
    public boolean supportsEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return domain.equals("yahoo.com") || domain.equals("yahoo.de") ||
               domain.equals("ymail.com") || domain.equals("rocketmail.com");
    }

    @Override
    public MailboxAccount createNewAccount(String email) {
        // Yahoo IMAP settings
        MailboxAccount account = new MailboxAccount(email, "imap.mail.yahoo.com", 993, email, "imaps");
        account.setAuthenticationType("OAUTH");
        return account;
    }

    @Override
    public String getAuthorizationUrl(String email) {
        // Yahoo OAuth 2.0 authorization endpoint and scopes
        // Scopes for IMAP access: "mail-r"
        String scope = "mail-r";
        return "https://api.login.yahoo.com/oauth2/request_auth" +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    @Override
    public void processCallback(MailboxAccount account, String code) throws Exception {
        // Yahoo OAuth 2.0 token endpoint
        String scope = "mail-r"; // Request offline_access for refresh tokens
        YahooOAuthClient.TokenResponse tokenResponse = yahooOAuthClient.getToken(
                clientId,
                clientSecret,
                redirectUri,
                code,
                "authorization_code",
                scope
        );

        account.setAccessToken(tokenResponse.access_token);
        account.setRefreshToken(tokenResponse.refresh_token);
        account.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.expires_in));
    }

    @Override
    public void refreshAccessToken(MailboxAccount account) throws Exception {
        LOG.info("Refreshing Yahoo OAuth token for " + account.getEmail() + "...");
        
        String scope = "mail-r"; // Ensure offline_access is requested for refresh tokens
        YahooOAuthClient.TokenResponse tokenResponse = yahooOAuthClient.refreshToken(
                clientId,
                clientSecret,
                account.getRefreshToken(),
                "refresh_token",
                scope
        );

        account.setAccessToken(tokenResponse.access_token);
        if (tokenResponse.refresh_token != null) {
            account.setRefreshToken(tokenResponse.refresh_token);
        }
        account.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.expires_in));
        
        LOG.info("Yahoo OAuth token successfully refreshed for " + account.getEmail());
    }
}