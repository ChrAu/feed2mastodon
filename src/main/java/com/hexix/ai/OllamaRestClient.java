package com.hexix.ai;


import com.hexix.ai.dto.EmbeddingRequest;
import com.hexix.ai.dto.EmbeddingResponse;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

//@Path("/api")
@RegisterRestClient(configKey = "ollama-api")
public interface OllamaRestClient {

    @POST // Der cURL-Befehl verwendet -d, was einem HTTP POST entspricht.
    @Path("/api/embed") // Der Pfad zum Endpunkt.
    @Consumes(MediaType.APPLICATION_JSON) // Wir senden JSON.
    @Produces(MediaType.APPLICATION_JSON) // Wir erwarten JSON als Antwort.
    EmbeddingResponse generateEmbeddings(EmbeddingRequest request);



    @POST // Der cURL-Befehl verwendet -d, was einem HTTP POST entspricht.
    @Path("/api/embed") // Der Pfad zum Endpunkt.
    @Consumes(MediaType.APPLICATION_JSON) // Wir senden JSON.
    @Produces(MediaType.APPLICATION_JSON) // Wir erwarten JSON als Antwort.
    Uni<EmbeddingResponse> generateEmbeddingsTest(EmbeddingRequest request);
}
