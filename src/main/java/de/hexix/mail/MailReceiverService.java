package de.hexix.mail;

import de.hexix.mail.model.MailboxAccount;
import de.hexix.mail.model.MailLogEntry;
import de.hexix.mail.oauth.OAuthTokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.search.BodyTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.transaction.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class MailReceiverService {

    private static final Logger LOG = Logger.getLogger(MailReceiverService.class.getName());
    // Angepasstes Pattern, um die Ticket-ID zu finden
    private static final Pattern UNIQUE_MAIL_ID_PATTERN = Pattern.compile("Ticket-ID: #(\\d{7})");

    @Inject
    MailboxAccountService mailboxAccountService;

    @Inject
    MailLogService mailLogService;

    @Inject
    OAuthTokenService oauthTokenService;

    @Transactional
    public void checkAllMailboxesForReceivedEmails() {
        LOG.info("Starting check for received emails in all configured mailboxes...");
        List<MailboxAccount> accounts = mailboxAccountService.getAllMailboxAccounts();

        if (accounts.isEmpty()) {
            LOG.info("No mailbox accounts configured for receiving emails.");
            return;
        }

        for (MailboxAccount account : accounts) {
            checkMailbox(account);
        }
        LOG.info("Finished checking all mailboxes.");
    }

    // Neue Methode: Überprüft ein spezifisches Postfach
    @Transactional
    public void checkMailboxForRecipient(String recipientEmail) {
        LOG.info("Starting check for received emails in mailbox: " + recipientEmail);
        MailboxAccount account = mailboxAccountService.getMailboxAccountByEmail(recipientEmail);

        if (account == null) {
            LOG.warning("No mailbox account found for email: " + recipientEmail + ". Skipping check.");
            return;
        }
        checkMailbox(account);
        LOG.info("Finished checking mailbox: " + recipientEmail);
    }


    private void checkMailbox(MailboxAccount account) {
        Store store = null;
        Folder folder = null;
        try {
            Properties props = new Properties();
            // Prefix should match the protocol used (e.g., "imaps" or "imap")
            String protocolPrefix = account.getProtocol().toLowerCase();

            props.put("mail." + protocolPrefix + ".ssl.enable", "true");
            props.put("mail." + protocolPrefix + ".host", account.getHost());
            props.put("mail." + protocolPrefix + ".port", String.valueOf(account.getPort()));
            props.put("mail." + protocolPrefix + ".auth", "true"); // Ensure authentication is enabled
            props.put("mail." + protocolPrefix + ".ssl.checkserveridentity", "true"); // Enable server identity check

            // OAuth 2.0 configuration
            if ("OAUTH".equalsIgnoreCase(account.getAuthenticationType())) {
                props.put("mail." + protocolPrefix + ".auth.mechanisms", "XOAUTH2");
                props.put("mail." + protocolPrefix + ".auth.plain.disable", "true");
                props.put("mail." + protocolPrefix + ".auth.login.disable", "true");
            }

            Session session = Session.getInstance(props);
            session.setDebug(false); // Set to true for detailed mail logging

            store = session.getStore(account.getProtocol());
            
            // Connect using the appropriate credentials
            if ("OAUTH".equalsIgnoreCase(account.getAuthenticationType())) {
                try {
                    // Generischer Token Refresh über den OAuthTokenService
                    oauthTokenService.refreshIfNecessary(account);
                } catch (Exception e) {
                    LOG.severe("Failed to ensure valid token for " + account.getEmail() + ": " + e.getMessage());
                    return; // Ohne gültiges Token abbrechen
                }

                // When using XOAUTH2 without an Authenticator, we must pass the token as the password
                store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getAccessToken());
            } else {
                store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());
            }

            // Protokolliere den letzten erfolgreichen Login
            account.setLastSuccessfulLogin(LocalDateTime.now());
            mailboxAccountService.updateMailboxAccount(account);
            LOG.info("Login successful for account: " + account.getEmail() + ". Last successful login updated.");

            folder = store.getFolder("INBOX"); // Or "inbox", depending on the mail server
            if (folder == null || !folder.exists()) {
                LOG.warning("Folder 'INBOX' not found for account: " + account.getEmail() + ". Trying 'inbox'.");
                folder = store.getFolder("inbox");
            }

            if (folder == null || !folder.exists()) {
                LOG.severe("Neither 'INBOX' nor 'inbox' folder found for account: " + account.getEmail() + "");
                return;
            }

            folder.open(Folder.READ_WRITE); // Open in READ_WRITE mode to allow deleting messages

            LOG.info("Checking mailbox: " + account.getEmail() + " for new messages...");

            // Search for emails that were sent by our MailService and are not yet marked as received
            List<MailLogEntry> pendingSentEmails = mailLogService.getPendingSentEmailsForRecipient(account.getEmail());

            for (MailLogEntry pendingEmail : pendingSentEmails) {
                Message[] messages = new Message[0];
                // First search attempt: "Ticket-ID: #<uniqueMailId>"
                SearchTerm searchTerm = new BodyTerm("Ticket-ID: #" + pendingEmail.getUniqueMailId());
                messages = folder.search(searchTerm);

                if (messages.length == 0) {
                    LOG.info("No message found for uniqueMailId with full pattern: " + pendingEmail.getUniqueMailId() + ". Trying fallback search.");
                    // Fallback search: just "<uniqueMailId>"
                    searchTerm = new BodyTerm(pendingEmail.getUniqueMailId());
                    messages = folder.search(searchTerm);
                }


                if (messages.length > 0) {
                    LOG.info("Found " + messages.length + " messages for uniqueMailId: " + pendingEmail.getUniqueMailId() + " in " + account.getEmail());
                    for (Message message : messages) {
                        String messageContent = (String) message.getContent(); // This might need more robust handling for multipart messages

                        // Extract uniqueMailId from the message content using regex
                        Matcher matcher = UNIQUE_MAIL_ID_PATTERN.matcher(messageContent);

                        if (matcher.find() && matcher.group(1).equals(pendingEmail.getUniqueMailId())) {
                            // Email found and unique ID matches
                            pendingEmail.setReceivedTimestamp(LocalDateTime.now());
                            pendingEmail.setReceivedStatus("RECEIVED");
                            pendingEmail.setReceptionCheckMessage("Email successfully received and identified.");
                            mailLogService.updateMailLogEntry(pendingEmail); // Update the log entry

                            // Mark for deletion
                            message.setFlag(Flags.Flag.DELETED, true);
                            LOG.info("Email with uniqueMailId: " + pendingEmail.getUniqueMailId() + " marked for deletion from " + account.getEmail());
                            break; // Found the email, no need to check other messages for this pendingEmail
                        }
                    }
                } else {
                    LOG.info("No message found for uniqueMailId: " + pendingEmail.getUniqueMailId() + " in " + account.getEmail());
                    // Optionally, update log entry to NOT_RECEIVED after a certain timeout
                }
            }

        } catch (NoSuchProviderException e) {
            LOG.severe("No provider for protocol " + account.getProtocol() + ": " + e.getMessage());
        } catch (MessagingException e) {
            LOG.severe("Messaging error for account " + account.getEmail() + ": " + e.getMessage());
        } catch (Exception e) {
            LOG.severe("Unexpected error checking mailbox " + account.getEmail() + ": " + e.getMessage());
        } finally {
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(true); // Close the folder and expunge (delete marked messages)
                }
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException e) {
                LOG.severe("Error closing mail resources for account " + account.getEmail() + ": " + e.getMessage());
            }
        }
    }
}