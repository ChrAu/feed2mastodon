package com.hexix;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/status")
public class StatusProxyResource {

    @Inject
    @RestClient
    KumaClient kumaClient;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() {
        JsonObject heartbeat = kumaClient.getHeartbeat();
        return Response.ok(heartbeat).build();
    }
}