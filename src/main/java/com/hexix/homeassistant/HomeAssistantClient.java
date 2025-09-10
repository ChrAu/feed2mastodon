package com.hexix.homeassistant;

import com.hexix.homeassistant.dto.EntityDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/api/")
@RegisterRestClient(baseUri = "https://homeassistant.codeheap.dev")
public interface HomeAssistantClient {

    /**
     * Ruft alle Zustände aller Entitäten ab.
     *
     * @param token Der Autorisierungs-Token (Bearer Token).
     * @return Eine Liste von {@link EntityDto}-Objekten, die die Zustände aller Entitäten darstellen.
     */
    @GET
    @Path("/states")
    @Produces(MediaType.APPLICATION_JSON)
    List<EntityDto> getAllStates(@HeaderParam("Authorization") String token);

    /**
     * Ruft den aktuellen Zustand einer bestimmten Entität ab.
     *
     * @param token    Der Autorisierungs-Token (Bearer Token).
     * @param entityId Die ID der Entität (z.B. "sensor.temperatur").
     * @return Ein {@link EntityDto}-Objekt, das den Zustand der Entität darstellt.
     */
    @GET
    @Path("/states/{entity_id}")
    @Produces(MediaType.APPLICATION_JSON)
    EntityDto getState(@HeaderParam("Authorization") String token, @PathParam("entity_id") String entityId);

    /**
     * NEUE METHODE:
     * Ruft den Verlauf aller Zustandsänderungen ab einem bestimmten Zeitstempel ab.
     * Der Zeitstempel muss im ISO 8601 Format sein (z.B. "2025-09-10T10:00:00+00:00").
     * Die Antwort ist eine Liste, die für jede Entität eine weitere Liste ihrer
     * historischen Zustände enthält.
     */
    @GET
    @Path("/history/period/{timestamp}")
    @Produces(MediaType.APPLICATION_JSON)
    List<List<EntityDto>> getHistorySince(@HeaderParam("Authorization") String token, @PathParam("timestamp") String timestamp, @QueryParam("filter_entity_id") String filterEntityId);

    @GET
    @Path("/history/period")
    @Produces(MediaType.APPLICATION_JSON)
    List<List<EntityDto>> getHistorySince(@HeaderParam("Authorization") String token, @QueryParam("filter_entity_id") String filterEntityId);
}
