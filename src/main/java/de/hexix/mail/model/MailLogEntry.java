package de.hexix.mail.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mail_log_entry")
@NamedQueries({
    @NamedQuery(name = MailLogEntry.QUERY_FIND_PENDING_FOR_RECIPIENT,
                query = "SELECT mle FROM MailLogEntry mle WHERE mle.recipientEmail = :recipientEmail AND mle.receivedStatus IS NULL AND mle.sentStatus = 'SUCCESS'"),
    @NamedQuery(name = MailLogEntry.QUERY_FIND_PENDING_OLDER_THAN,
                query = "SELECT mle FROM MailLogEntry mle WHERE mle.receivedStatus IS NULL AND mle.sentTimestamp < :threshold AND mle.sentStatus = 'SUCCESS'"),
        @NamedQuery(name = MailLogEntry.QUERY_FIND_ORDER_BY_SENT_TIMESTAMP_DESC, query = "SELECT mle FROM MailLogEntry mle ORDER BY mle.sentTimestamp DESC")
})
public class MailLogEntry { // PanacheEntityBase entfernt


    public static final String QUERY_FIND_PENDING_FOR_RECIPIENT = "MailLogEntry.findPendingForRecipient";
    public static final String QUERY_FIND_PENDING_OLDER_THAN = "MailLogEntry.findPendingOlderThan";
    public static final String QUERY_FIND_ORDER_BY_SENT_TIMESTAMP_DESC = "MailLogEntry.findOrderBySentTimestampDesc";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String uniqueMailId; // UUID for the batch of emails

    @Column(nullable = false)
    public String recipientEmail;

    @Column(nullable = false)
    public LocalDateTime sentTimestamp;

    @Column(nullable = false)
    public String sentStatus; // e.g., "SUCCESS", "FAILED"

    @Column(columnDefinition = "TEXT")
    public String errorMessage; // If sending failed

    public LocalDateTime receivedTimestamp; // When the email was confirmed as received

    public String receivedStatus; // e.g., "RECEIVED", "NOT_RECEIVED", "ERROR"

    @Column(columnDefinition = "TEXT")
    public String receptionCheckMessage; // Any message from the reception check

    // Default constructor for JPA
    public MailLogEntry() {
    }

    public MailLogEntry(String uniqueMailId, String recipientEmail, LocalDateTime sentTimestamp, String sentStatus, String errorMessage) {
        this.uniqueMailId = uniqueMailId;
        this.recipientEmail = recipientEmail;
        this.sentTimestamp = sentTimestamp;
        this.sentStatus = sentStatus;
        this.errorMessage = errorMessage;
    }

    // Getter und Setter für alle Felder (für klassischen JPA-Ansatz)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniqueMailId() {
        return uniqueMailId;
    }

    public void setUniqueMailId(String uniqueMailId) {
        this.uniqueMailId = uniqueMailId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public LocalDateTime getSentTimestamp() {
        return sentTimestamp;
    }

    public void setSentTimestamp(LocalDateTime sentTimestamp) {
        this.sentTimestamp = sentTimestamp;
    }

    public String getSentStatus() {
        return sentStatus;
    }

    public void setSentStatus(String sentStatus) {
        this.sentStatus = sentStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public void setReceivedTimestamp(LocalDateTime receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    public String getReceivedStatus() {
        return receivedStatus;
    }

    public void setReceivedStatus(String receivedStatus) {
        this.receivedStatus = receivedStatus;
    }

    public String getReceptionCheckMessage() {
        return receptionCheckMessage;
    }

    public void setReceptionCheckMessage(String receptionCheckMessage) {
        this.receptionCheckMessage = receptionCheckMessage;
    }
}
