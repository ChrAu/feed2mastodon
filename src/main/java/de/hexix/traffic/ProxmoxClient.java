package de.hexix.traffic;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api2/json")
@RegisterRestClient(configKey = "proxmox-api")
public interface ProxmoxClient {
    @GET
    @Path("/nodes/{node}/status")
    JsonNode getNodeStatus(
            @PathParam("node") String node,
            @HeaderParam("Authorization") String authHeader
    );

    @GET
    @Path("/nodes/{node}/netstat")
    JsonNode getNetstatStatus(@PathParam("node") String node, @HeaderParam("Authorization") String auth);

    @GET
    @Path("/nodes/{node}/network")
    JsonNode getNetworkStatus(@PathParam("node") String node, @HeaderParam("Authorization") String auth);

    @GET
    @Path("/nodes/{node}/rrddata")
// timeframe kann 'hour', 'day' etc. sein. Wir nehmen 'hour' für die aktuellsten Punkte.
    JsonNode getRrdData(
            @PathParam("node") String node,
            @QueryParam("timeframe") String timeframe,
            @HeaderParam("Authorization") String auth
    );

    @GET
    @Path("/cluster/resources")
// Optional: Mit @QueryParam("type") "node" oder "vm" filtern
    JsonNode getClusterResources(@QueryParam("type") String type, @HeaderParam("Authorization") String auth);
}
