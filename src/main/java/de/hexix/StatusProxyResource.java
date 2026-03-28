package de.hexix;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/status")
public class StatusProxyResource {

    @Inject
    @RestClient
    KumaClient kumaClient;

    @GET
    @Produces("image/svg+xml")
    public Response getStatus(@QueryParam("id") String id) {
        // Fallback to ID 7 if no ID is provided, consistent with previous behavior
        String monitorId = (id != null && !id.isEmpty()) ? id : "7";
        
        try {
            String badgeSvg = kumaClient.getStatusBadge(monitorId);
            return Response.ok(badgeSvg).build();
        } catch (Exception e) {
            return Response.serverError().entity("Error fetching status").build();
        }
    }
    
    @GET
    @Path("/page")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusPage() {
        try {
            String statusPageJson = kumaClient.getStatusPage();
            return Response.ok(statusPageJson).build();
        } catch (Exception e) {
            return Response.serverError().entity("Error fetching status page").build();
        }
    }
}