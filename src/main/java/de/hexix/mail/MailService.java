package de.hexix.mail;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class.getName());

    @Inject
    Mailer mailer; // Inject the Quarkus Mailer

    @ConfigProperty(name = "quarkus.mailer.from")
    String mailerFrom; // Inject the 'from' address from configuration

    // Angepasste Methode, um uniqueMailId zu akzeptieren
    public void sendEmail(String recipient, String subject, String body, String uniqueMailId) {
        try {
            String messageId = "<" + UUID.randomUUID().toString() + "@codeheap.dev>";
            mailer.send(Mail.withText(recipient, subject, body)
                    .setFrom(mailerFrom)
                    .addHeader("Message-ID", messageId));
            LOG.info("Email sent successfully to " + recipient + " with subject: " + subject + " (ID: " + uniqueMailId + ")");
        } catch (Exception e) {
            LOG.severe("Failed to send email to " + recipient + ": " + e.getMessage() + " (ID: " + uniqueMailId + ")");
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public String getMailerFrom() {
        return mailerFrom;
    }
}
