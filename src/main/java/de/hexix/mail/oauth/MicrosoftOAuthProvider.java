package de.hexix.mail.oauth;

import de.hexix.mail.MicrosoftOAuthClient;
import de.hexix.mail.model.MailboxAccount;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

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

    @Override
    public boolean supports(MailboxAccount account) {
        // This provider is for Microsoft accounts (Outlook, Office365)
        return account.getHost() != null && account.getHost().contains("office365.com");
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