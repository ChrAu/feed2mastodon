package de.hexix.mail;

import de.hexix.mail.model.MailLogEntry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class MailTimeoutScheduler {

    private static final Logger LOG = Logger.getLogger(MailTimeoutScheduler.class.getName());

    @Inject
    MailLogService mailLogService;

    /**
     * Überprüft regelmäßig, ob gesendete E-Mails nach 24 Stunden noch nicht empfangen wurden.
     * Der Cron-Ausdruck "0 0 * / 6 * * ?" bedeutet:
     * Jede Stunde 0, Minute 0, alle 6 Stunden (00:00, 06:00, 12:00, 18:00 Uhr).
     * Dies entspricht 4 Überprüfungen pro Tag.
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    void checkMailTimeouts() {
        LOG.info("Starting scheduled check for mail timeouts...");

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minus(24, ChronoUnit.HOURS);

        List<MailLogEntry> timedOutEmails = mailLogService.getPendingEmailsOlderThan(twentyFourHoursAgo);

        if (timedOutEmails.isEmpty()) {
            LOG.info("No timed-out emails found.");
            return;
        }

        LOG.info("Found " + timedOutEmails.size() + " timed-out emails.");
        for (MailLogEntry entry : timedOutEmails) {
            entry.setReceivedStatus("FAILED_TIMEOUT");
            entry.setReceptionCheckMessage("Email not received within 24 hours timeout.");
            mailLogService.updateMailLogEntry(entry);
            LOG.warning("Mail with uniqueMailId: " + entry.getUniqueMailId() + " to " + entry.getRecipientEmail() + " timed out.");
        }
        LOG.info("Finished scheduled check for mail timeouts.");
    }
}
