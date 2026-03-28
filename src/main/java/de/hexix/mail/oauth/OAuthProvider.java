package de.hexix.mail.oauth;

import de.hexix.mail.model.MailboxAccount;

public interface OAuthProvider {
    
    /**
     * Eindeutiger Bezeichner des Providers (z. B. "microsoft").
     * Wird verwendet, um beim Login den richtigen Provider auszuwählen.
     */
    String getProviderId();

    /**
     * Prüft, ob dieser Provider für das gegebene Konto zuständig ist.
     * Zum Beispiel anhand des Hostnamens.
     */
    boolean supports(MailboxAccount account);

    /**
     * Prüft, ob dieser Provider für die gegebene E-Mail-Adresse zuständig ist.
     * Zum Beispiel anhand der Domain.
     */
    boolean supportsEmail(String email);

    /**
     * Erstellt ein neues Account-Objekt mit den passenden Standardwerten (Host, Port, Protokoll) für diesen Provider.
     */
    MailboxAccount createNewAccount(String email);

    /**
     * Generiert die Authorization URL für den Login-Vorgang, um den Nutzer weiterzuleiten.
     */
    String getAuthorizationUrl(String email);

    /**
     * Verarbeitet den Code aus dem OAuth-Callback und setzt die Tokens im Account.
     */
    void processCallback(MailboxAccount account, String code) throws Exception;

    /**
     * Erneuert das Access-Token für das gegebene Konto mithilfe des Refresh-Tokens.
     * 
     * @param account Das Mail-Konto, dessen Token erneuert werden soll.
     * @throws Exception Falls bei der Erneuerung ein Fehler auftritt.
     */
    void refreshAccessToken(MailboxAccount account) throws Exception;
}