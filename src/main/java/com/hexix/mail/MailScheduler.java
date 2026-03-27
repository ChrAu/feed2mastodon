package com.hexix.mail;

import com.hexix.mail.model.MailLogEntry;
import com.hexix.mail.model.MailboxAccount; // Import für MailboxAccount
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List; // Nur noch List, kein Arrays.asList mehr
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class MailScheduler {

    private static final Logger LOG = Logger.getLogger(MailScheduler.class.getName());

    @Inject
    MailService mailService;

    @Inject
    MailLogService mailLogService;

    @Inject
    MailboxAccountService mailboxAccountService; // Inject MailboxAccountService

    @Inject
    MailReceiverService mailReceiverService; // Inject MailReceiverService

    /**
     * Sendet Test-E-Mails an die konfigurierten Empfänger aus der Datenbank.
     * Der Cron-Ausdruck "0 0 0,6,12,18 * * ?" bedeutet:
     * Jede Stunde 0, Minute 0, um 00:00, 06:00, 12:00 und 18:00 Uhr.
     * Dies entspricht 4 E-Mails pro Tag.
     */
    @Scheduled(cron = "0 0 0,6,12,18 * * ?") // Zurück zum ursprünglichen Cron-Ausdruck
    void sendScheduledTestEmails() {
        LOG.info("Starting scheduled email sending...");
        String uniqueMailId = UUID.randomUUID().toString();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime sentTime = LocalDateTime.now();

        List<MailboxAccount> recipientAccounts = mailboxAccountService.getAllMailboxAccounts(); // Empfänger aus DB laden

        if (recipientAccounts.isEmpty()) {
            LOG.warning("No mailbox accounts found in the database. Skipping scheduled email sending.");
            return;
        }

        for (MailboxAccount account : recipientAccounts) { // Iteriere über die geladenen Accounts
            String recipient = account.getEmail(); // E-Mail-Adresse des Empfängers
            String subject = String.format("Spam-Test: Mailserver-Check [%s] - %s", uniqueMailId, timestamp);
            String body = String.format(
                    "Dies ist eine geplante Test-E-Mail von Ihrem Mailserver (%s) zur Überprüfung der Spam-Erkennung.\n" +
                    "Eindeutige Mail-ID: %s\n" +
                    "Gesendet am: %s\n\n" +
                    "Bitte nicht antworten.",
                    mailService.getMailerFrom(),
                    uniqueMailId,
                    timestamp
            );

            String sentStatus = "SUCCESS";
            String errorMessage = null;

            try {
                mailService.sendEmail(recipient, subject, body, uniqueMailId);
            } catch (Exception e) {
                LOG.severe("Failed to send scheduled email to " + recipient + ": " + e.getMessage());
                sentStatus = "FAILED";
                errorMessage = e.getMessage();
            } finally {
                MailLogEntry logEntry = new MailLogEntry(
                        uniqueMailId,
                        recipient,
                        mailService.getMailerFrom(),
                        subject,
                        body.substring(0, Math.min(body.length(), 255)),
                        sentTime,
                        sentStatus,
                        errorMessage
                );
                mailLogService.logMailSendAttempt(logEntry);

                // Sofortige Überprüfung des Empfangs für diesen Empfänger
                if ("SUCCESS".equals(sentStatus)) {
                    mailReceiverService.checkMailboxForRecipient(recipient);
                }
            }
        }
        LOG.info("Finished scheduled email sending for ID: " + uniqueMailId);
    }
}
