package com.hexix;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://kuma.codeheap.dev")
public interface KumaClient {

    @GET
    @Path("/api/badge/{id}/status")
    @Produces("image/svg+xml")
    String getStatusBadge(@PathParam("id") String id);
}