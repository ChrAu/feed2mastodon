package de.hexix.mail;

import de.hexix.mail.model.dto.MailProviderStats;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/mail-test")
public class MailTestResource {

    @Inject
    MailService mailService;

    @Inject
    MailLogService mailLogService; // Inject MailLogService

    @Inject
    MailReceiverService mailReceiverService; // Inject MailReceiverService

    @Inject
    MailScheduler mailScheduler; // Inject MailScheduler

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendTestEmail() {
        mailScheduler.sendScheduledTestEmails();
        return Response.ok("Manuelle Überprüfung des E-Mails senden, gestartet.").build();
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

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MailProviderStats> getMailStats() {
        return mailLogService.getMailProviderStatistics();
    }
}
