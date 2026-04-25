package de.hexix.mail;

import de.hexix.mail.model.MailLogEntry;
import de.hexix.mail.model.MailboxAccount; // Import für MailboxAccount
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List; // Nur noch List, kein Arrays.asList mehr
import java.util.logging.Logger;

@ApplicationScoped
public class MailScheduler {

    private static final Logger LOG = Logger.getLogger(MailScheduler.class.getName());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        String uniqueMailId = String.format("%07d", SECURE_RANDOM.nextInt(10000000));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime sentTime = LocalDateTime.now();

        List<MailboxAccount> recipientAccounts = mailboxAccountService.getAllMailboxAccounts(); // Empfänger aus DB laden

        if (recipientAccounts.isEmpty()) {
            LOG.warning("No mailbox accounts found in the database. Skipping scheduled email sending.");
            return;
        }

        for (MailboxAccount account : recipientAccounts) { // Iteriere über die geladenen Accounts
            String recipient = account.getEmail(); // E-Mail-Adresse des Empfängers
            String subject = "Kurze Rückfrage zu unserem Termin am Montag";
            String body = String.format(
                    "Hallo Christopher,\n\n" +
                    "ich wollte mich nur kurz erkundigen, ob die Unterlagen für unser Gespräch am Montag bereits eingetroffen sind.\n\n" +
                    "Gib mir bitte kurz Bescheid, falls ich noch etwas vorbereiten soll.\n\n" +
                    "Beste Grüße,\n" +
                    "Dein Codeheap-Team\n\n" +
                    "Ticket-ID: #%s",
                    uniqueMailId
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
