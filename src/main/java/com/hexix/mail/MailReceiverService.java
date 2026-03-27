package com.hexix.mail;

import com.hexix.mail.model.MailboxAccount;
import com.hexix.mail.model.MailLogEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.BodyTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
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
    private static final Pattern UNIQUE_MAIL_ID_PATTERN = Pattern.compile("--- Unique Mail ID: ([a-fA-F0-9-]+) ---");

    @Inject
    MailboxAccountService mailboxAccountService;

    @Inject
    MailLogService mailLogService;

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
        MailboxAccount account = mailboxAccountService.getMailboxAccountByEmail(recipientEmail); // Diese Methode müssen wir noch hinzufügen

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
            // Enable SSL/TLS for IMAP/POP3
            String protocolPrefix = account.getProtocol().substring(0, account.getProtocol().length() - 1); // e.g., "imap" from "imaps"

            props.put("mail." + protocolPrefix + ".ssl.enable", "true");
            props.put("mail." + protocolPrefix + ".host", account.getHost());
            props.put("mail." + protocolPrefix + ".port", String.valueOf(account.getPort()));
            props.put("mail." + protocolPrefix + ".auth", "true"); // Ensure authentication is enabled
            props.put("mail." + protocolPrefix + ".ssl.checkserveridentity", "true"); // Enable server identity check

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(account.getUsername(), account.getPassword());
                }
            });
            session.setDebug(false); // Set to true for detailed mail logging

            store = session.getStore(account.getProtocol());
            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());

            folder = store.getFolder("INBOX"); // Or "inbox", depending on the mail server
            if (folder == null || !folder.exists()) {
                LOG.warning("Folder 'INBOX' not found for account: " + account.getEmail() + ". Trying 'inbox'.");
                folder = store.getFolder("inbox");
            }

            if (folder == null || !folder.exists()) {
                LOG.severe("Neither 'INBOX' nor 'inbox' folder found for account: " + account.getEmail());
                return;
            }

            folder.open(Folder.READ_WRITE); // Open in READ_WRITE mode to allow deleting messages

            LOG.info("Checking mailbox: " + account.getEmail() + " for new messages...");

            // Search for emails that were sent by our MailService and are not yet marked as received
            List<MailLogEntry> pendingSentEmails = mailLogService.getPendingSentEmailsForRecipient(account.getEmail());

            for (MailLogEntry pendingEmail : pendingSentEmails) {
                // Create a search term that looks for the uniqueMailId in the body
                // Note: SubjectTerm is less reliable as subjects can be altered or truncated.
                // BodyTerm is better for the unique ID.
                SearchTerm searchTerm = new BodyTerm(pendingEmail.getUniqueMailId());

                Message[] messages = folder.search(searchTerm);

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
