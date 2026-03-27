package com.hexix.mail;

import com.hexix.mail.model.MailLogEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
}
