package de.hexix.mail.model.dto;

import java.time.LocalDateTime;

public class MailProviderStats {
    public String provider;
    public LocalDateTime lastSent;
    public String lastSentStatus; // Status des Sendens (SUCCESS/FAILED)
    public String lastMailReceptionStatus; // Status des Empfangs (RECEIVED/NOT_RECEIVED/ERROR/PENDING_CHECK/N/A)
    public long failuresLast7Days;

    public MailProviderStats(String provider, LocalDateTime lastSent, String lastSentStatus, String lastMailReceptionStatus, long failuresLast7Days) {
        this.provider = provider;
        this.lastSent = lastSent;
        this.lastSentStatus = lastSentStatus;
        this.lastMailReceptionStatus = lastMailReceptionStatus;
        this.failuresLast7Days = failuresLast7Days;
    }
}
