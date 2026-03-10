package com.hexix;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/status")
public class StatusProxyResource {

    @Inject
    @RestClient
    KumaClient kumaClient;

    @GET
    @Produces("image/svg+xml")
    public Response getStatus() {
        String badgeSvg = kumaClient.getStatusBadge();
        return Response.ok(badgeSvg).build();
    }
}