package com.hexix.mail;

import com.hexix.mail.model.MailLogEntry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.UUID;

@Path("/api/mail-test")
public class MailTestResource {

    @Inject
    MailService mailService;

    @Inject
    MailLogService mailLogService; // Inject MailLogService

    @Inject
    MailReceiverService mailReceiverService; // Inject MailReceiverService

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendTestEmail() {
        String recipient = "auth@hexix.de";
        String subject = "Test-E-Mail von Feed2Mastodon";
        String body = "Dies ist eine Test-E-Mail, die von Ihrer Feed2Mastodon-Anwendung gesendet wurde, um den MailService zu überprüfen.";

        String uniqueMailId = UUID.randomUUID().toString();
        LocalDateTime sentTime = LocalDateTime.now();
        String sentStatus = "SUCCESS";
        String errorMessage = null;

        try {
            mailService.sendEmail(recipient, subject, body, uniqueMailId); // uniqueMailId hinzugefügt
            return Response.ok("Test-E-Mail erfolgreich an " + recipient + " gesendet.").build();
        } catch (Exception e) {
            sentStatus = "FAILED";
            errorMessage = e.getMessage();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Fehler beim Senden der Test-E-Mail: " + e.getMessage())
                           .build();
        } finally {
            // Log the mail send attempt regardless of success or failure
            MailLogEntry logEntry = new MailLogEntry(
                    uniqueMailId,
                    recipient,
                    mailService.getMailerFrom(),
                    subject,
                    body.substring(0, Math.min(body.length(), 255)), // Log a snippet of the body
                    sentTime,
                    sentStatus,
                    errorMessage
            );
            mailLogService.logMailSendAttempt(logEntry);
        }
    }

    @GET
    @Path("/check-received") // Neuer Endpunkt
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkReceivedEmailsManually() {
        try {
            mailReceiverService.checkAllMailboxesForReceivedEmails();
            return Response.ok("Manuelle Überprüfung der empfangenen E-Mails gestartet.").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Fehler bei der manuellen Überprüfung der empfangenen E-Mails: " + e.getMessage())
                           .build();
        }
    }
}
