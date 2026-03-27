package com.hexix.mail;

import com.hexix.mail.model.MailLogEntry;
import com.hexix.mail.model.dto.MailProviderStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@ApplicationScoped
public class MailLogService {

    private static final Logger LOG = Logger.getLogger(MailLogService.class.getName());

    @Inject
    EntityManager entityManager;

    @Transactional
    public void logMailSendAttempt(MailLogEntry logEntry) {
        try {
            entityManager.persist(logEntry);
            LOG.info("Mail log entry persisted for uniqueMailId: " + logEntry.getUniqueMailId() + ", recipient: " + logEntry.getRecipientEmail());
        } catch (Exception e) {
            LOG.severe("Failed to persist mail log entry for uniqueMailId: " + logEntry.getUniqueMailId() + ", recipient: " + logEntry.getRecipientEmail() + ": " + e.getMessage());
            throw new RuntimeException("Failed to persist mail log entry", e);
        }
    }

    // Gibt ausstehende gesendete E-Mails für einen Empfänger zurück
    public List<MailLogEntry> getPendingSentEmailsForRecipient(String recipientEmail) {
        return entityManager.createNamedQuery(
                        MailLogEntry.QUERY_FIND_PENDING_FOR_RECIPIENT,
                        MailLogEntry.class)
                .setParameter("recipientEmail", recipientEmail)
                .getResultList();
    }

    // Neue Methode: Gibt ausstehende E-Mails zurück, die älter als ein bestimmter Zeitpunkt sind
    public List<MailLogEntry> getPendingEmailsOlderThan(LocalDateTime threshold) {
        return entityManager.createNamedQuery(
                        MailLogEntry.QUERY_FIND_PENDING_OLDER_THAN,
                        MailLogEntry.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    // Aktualisiert einen MailLogEntry
    @Transactional
    public MailLogEntry updateMailLogEntry(MailLogEntry logEntry) {
        try {
            MailLogEntry updatedEntry = entityManager.merge(logEntry);
            LOG.info("Mail log entry updated for uniqueMailId: " + logEntry.getUniqueMailId() + ", recipient: " + logEntry.getRecipientEmail());
            return updatedEntry;
        } catch (Exception e) {
            LOG.severe("Failed to update mail log entry for uniqueMailId: " + logEntry.getUniqueMailId() + ", recipient: " + logEntry.getRecipientEmail() + ": " + e.getMessage());
            throw new RuntimeException("Failed to update mail log entry", e);
        }
    }

    public List<MailProviderStats> getMailProviderStatistics() {
        List<MailLogEntry> allEntries = entityManager.createNamedQuery(MailLogEntry.QUERY_FIND_ORDER_BY_SENT_TIMESTAMP_DESC, MailLogEntry.class)
                                                     .getResultList();

        Map<String, MailProviderStats> statsMap = new HashMap<>();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime seventyMinutesAgo = LocalDateTime.now().minus(70, ChronoUnit.MINUTES);

        for (MailLogEntry entry : allEntries) {
            String recipientEmail = entry.getRecipientEmail();
            String provider = extractProviderFromEmail(recipientEmail);

            // Ensure the provider is initialized in the map
            statsMap.computeIfAbsent(provider, k -> new MailProviderStats(k, null, null, null, 0));
            MailProviderStats stats = statsMap.get(provider);

            // Update last sent info (since entries are ordered by sentTimestamp DESC, the first one encountered is the latest)
            // This block ensures that 'lastSent' and 'lastSentStatus' are set only once for the latest entry of a provider
            if (stats.lastSent == null) {
                stats.lastSent = entry.getSentTimestamp();
                stats.lastSentStatus = entry.getSentStatus();

                // Determine lastMailReceptionStatus for the latest email
                if (entry.getReceivedStatus() == null) {
                    if (entry.getSentTimestamp().isAfter(seventyMinutesAgo)) {
                        stats.lastMailReceptionStatus = "PENDING_CHECK";
                    } else {
                        stats.lastMailReceptionStatus = "N/A"; // Older than 70 min and not received
                    }
                } else {
                    stats.lastMailReceptionStatus = entry.getReceivedStatus();
                }
            }

            // Count failures in the last 7 days
            if (entry.getSentTimestamp().isAfter(sevenDaysAgo) && "FAILED".equals(entry.getSentStatus())) {
                stats.failuresLast7Days++;
            }
        }
        return new ArrayList<>(statsMap.values());
    }

    private String extractProviderFromEmail(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(email.indexOf("@") + 1);
        }
        return "unknown"; // Default for invalid emails
    }
}
