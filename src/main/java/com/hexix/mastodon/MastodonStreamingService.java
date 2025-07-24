package com.hexix.mastodon;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Quarkus RestClient Interface für die Mastodon Streaming API.
 * Hört auf den öffentlichen Zeitstrahl mittels Server-Sent Events (SSE).
 * Liefert jeden empfangenen SSE-Ereignis-Payload als String.
 */
@RegisterRestClient(baseUri = "https://mastodon.hexix.de")
// Fügt den Authorization Bearer Token als Header hinzu.
// In einer Produktionsumgebung sollte der Token nicht hartkodiert sein,
// sondern z.B. aus der Konfiguration oder einem Secrets-Management-System geladen werden.
public interface MastodonStreamingService {

    /**
     * Streamt den öffentlichen Zeitstrahl von Mastodon.
     * Der Endpunkt ist /api/v1/streaming/public.
     *
     * @return Ein Multi<String>, das jeden empfangenen SSE-Ereignis-Payload (den "data"-Teil) als String liefert.
     */
    @GET
    @Path("/api/v1/streaming/public")
    @Produces(MediaType.SERVER_SENT_EVENTS) // Wichtig: Definiert den Medientyp als Server-Sent Events
    Multi<String> streamPublicTimeline(@HeaderParam("Authorization") String accessToken);

    /**
     * Streamt den direct Zeitstrahl von Mastodon.
     * Der Endpunkt ist /api/v1/streaming/direct.
     *
     * @return Ein Multi<String>, das jeden empfangenen SSE-Ereignis-Payload (den "data"-Teil) als String liefert.
     */
    @GET
    @Path("/api/v1/streaming/direct")
    @Produces(MediaType.SERVER_SENT_EVENTS) // Wichtig: Definiert den Medientyp als Server-Sent Events
    Multi<String> mastodonStreamingDirect(@HeaderParam("Authorization") String accessToken);
}
