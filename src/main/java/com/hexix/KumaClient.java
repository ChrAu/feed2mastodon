package com.hexix;

import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://kuma.codeheap.dev")
public interface KumaClient {

    @GET
    @Path("/api/status-page/heartbeat/codeheap")
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject getHeartbeat();
}