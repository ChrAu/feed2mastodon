package de.hexix.mail.oauth;

import de.hexix.mail.MicrosoftOAuthClient;
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
public class MicrosoftOAuthProvider implements OAuthProvider {

    private static final Logger LOG = Logger.getLogger(MicrosoftOAuthProvider.class.getName());

    @Inject
    @RestClient
    MicrosoftOAuthClient microsoftOAuthClient;

    @ConfigProperty(name = "microsoft.oauth.client.id")
    String clientId;

    @ConfigProperty(name = "microsoft.oauth.client.secret")
    String clientSecret;

    @ConfigProperty(name = "microsoft.oauth.tenant.id")
    String tenantId;

    @ConfigProperty(name = "microsoft.oauth.redirect.uri")
    String redirectUri;

    @Override
    public String getProviderId() {
        return "microsoft";
    }

    @Override
    public boolean supports(MailboxAccount account) {
        // This provider is for Microsoft accounts (Outlook, Office365)
        return account.getHost() != null && account.getHost().contains("office365.com");
    }

    @Override
    public boolean supportsEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        return domain.equals("outlook.com") || domain.equals("outlook.de") ||
               domain.equals("hotmail.com") || domain.equals("hotmail.de") ||
               domain.equals("office365.com") || domain.equals("live.com") ||
               domain.equals("live.de");
    }

    @Override
    public MailboxAccount createNewAccount(String email) {
        MailboxAccount account = new MailboxAccount(email, "outlook.office365.com", 993, email, "imaps");
        account.setAuthenticationType("OAUTH");
        return account;
    }

    @Override
    public String getAuthorizationUrl(String email) {
        String scope = "https://outlook.office.com/IMAP.AccessAsUser.All offline_access";
        return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/authorize" +
                "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_mode=query" +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    @Override
    public void processCallback(MailboxAccount account, String code) throws Exception {
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
    }

    @Override
    public void refreshAccessToken(MailboxAccount account) throws Exception {
        LOG.info("Refreshing Microsoft OAuth token for " + account.getEmail() + "...");
        
        String scope = "https://outlook.office.com/IMAP.AccessAsUser.All offline_access";
        MicrosoftOAuthClient.TokenResponse tokenResponse = microsoftOAuthClient.refreshToken(
                tenantId,
                clientId,
                scope,
                account.getRefreshToken(),
                "refresh_token",
                clientSecret
        );

        account.setAccessToken(tokenResponse.access_token);
        if (tokenResponse.refresh_token != null) {
            account.setRefreshToken(tokenResponse.refresh_token);
        }
        account.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.expires_in));
        
        LOG.info("Microsoft OAuth token successfully refreshed for " + account.getEmail());
    }
}