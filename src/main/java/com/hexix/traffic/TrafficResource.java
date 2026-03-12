package com.hexix.traffic;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/traffic-stream")
public class TrafficResource {
    @Inject
    TrafficService trafficService;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<ServerMetrics> streamTraffic() {
        // Alle Clients rufen dieselbe Methode auf,
        // aber der Service fragt Proxmox nur einmal ab.
        return trafficService.getSharedStream();
    }
}
