package de.hexix.mail.oauth;

import de.hexix.mail.model.MailboxAccount;

public interface OAuthProvider {
    
    /**
     * Prüft, ob dieser Provider für das gegebene Konto zuständig ist.
     * Zum Beispiel anhand des Hostnamens.
     */
    boolean supports(MailboxAccount account);

    /**
     * Erneuert das Access-Token für das gegebene Konto mithilfe des Refresh-Tokens.
     * 
     * @param account Das Mail-Konto, dessen Token erneuert werden soll.
     * @throws Exception Falls bei der Erneuerung ein Fehler auftritt.
     */
    void refreshAccessToken(MailboxAccount account) throws Exception;
}