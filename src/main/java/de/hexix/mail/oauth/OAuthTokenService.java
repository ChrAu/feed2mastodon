package de.hexix.mail.oauth;

import de.hexix.mail.model.MailboxAccount;
import de.hexix.mail.MailboxAccountService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@ApplicationScoped
public class OAuthTokenService {

    private static final Logger LOG = Logger.getLogger(OAuthTokenService.class.getName());

    @Inject
    Instance<OAuthProvider> oauthProviders;

    @Inject
    MailboxAccountService mailboxAccountService;

    /**
     * Sucht den passenden Provider anhand seiner eindeutigen ID.
     */
    public OAuthProvider getProviderById(String providerId) {
        if (providerId == null || providerId.isEmpty()) {
            return null;
        }
        for (OAuthProvider provider : oauthProviders) {
            if (providerId.equalsIgnoreCase(provider.getProviderId())) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Sucht den passenden Provider anhand des konfigurierten Kontos (z. B. anhand des Hostnamens).
     */
    public OAuthProvider getProviderForAccount(MailboxAccount account) {
        for (OAuthProvider provider : oauthProviders) {
            if (provider.supports(account)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Sucht den passenden Provider anhand der E-Mail-Adresse (z. B. anhand der Domain).
     */
    public OAuthProvider getProviderByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        for (OAuthProvider provider : oauthProviders) {
            if (provider.supportsEmail(email)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Prüft, ob das Token abgelaufen ist, und erneuert es über den passenden Provider.
     * Speichert die Änderungen danach direkt im MailboxAccountService.
     * 
     * @param account Das E-Mail Konto
     * @throws Exception Wenn kein Refresh-Token da ist oder der Refresh-Prozess fehlschlägt
     */
    public void refreshIfNecessary(MailboxAccount account) throws Exception {
        // Nicht-OAuth-Konten ignorieren
        if (!"OAUTH".equalsIgnoreCase(account.getAuthenticationType())) {
            return;
        }

        // Prüfen, ob das Token überhaupt abgelaufen ist
        if (account.getAccessTokenExpiry() == null || account.getAccessTokenExpiry().isAfter(LocalDateTime.now())) {
            return; // Token ist noch gültig
        }

        if (account.getRefreshToken() == null) {
            throw new Exception("Access token expired for " + account.getEmail() + " but no refresh token is available.");
        }

        LOG.info("Access token expired for " + account.getEmail() + ". Searching for suitable OAuthProvider...");

        OAuthProvider provider = getProviderForAccount(account);
        if (provider != null) {
            LOG.info("Using " + provider.getClass().getSimpleName() + " to refresh token.");
            provider.refreshAccessToken(account);
            
            // Änderungen sofort in die DB schreiben
            mailboxAccountService.updateMailboxAccount(account);
            return;
        }

        throw new Exception("No suitable OAuthProvider found to refresh token for account: " + account.getEmail() + " with host " + account.getHost());
    }
}