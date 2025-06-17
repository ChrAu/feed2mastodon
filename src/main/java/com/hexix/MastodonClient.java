package com.hexix;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.ZonedDateTime;
import java.util.List;

@Path("/api/v1")
@RegisterRestClient
public interface MastodonClient {

    @POST
    @Path("/statuses")
    @Produces(MediaType.APPLICATION_JSON)
    MastodonStatus postStatus(@HeaderParam("Authorization") String accessToken, StatusPayload status);

    /**
     * Ruft die Account-Informationen ab, die mit dem Access Token verknüpft sind.
     * Nützlich, um die eigene Account-ID zu erhalten.
     * @param accessToken Der "Bearer <TOKEN>" String.
     * @return Ein MastodonAccount-Objekt.
     */
    @GET
    @Path("/accounts/verify_credentials")
    MastodonAccount verifyCredentials(@HeaderParam("Authorization") String accessToken);

    /**
     * Ruft eine Liste der Status-Updates für eine gegebene Account-ID ab.
     * @param accessToken Der "Bearer <TOKEN>" String.
     * @param accountId Die ID des Accounts, dessen Posts abgerufen werden sollen.
     * @param limit Die maximale Anzahl an Posts, die zurückgegeben werden soll (optional).
     * @return Eine Liste von MastodonStatus-Objekten.
     */
    @GET
    @Path("/accounts/{id}/statuses")
    List<MastodonStatus> getAccountStatuses(
            @HeaderParam("Authorization") String accessToken,
            @PathParam("id") String accountId,
            @QueryParam("limit") Integer limit
    );

    /**
     * Löscht einen bestimmten Status anhand seiner ID.
     * @param accessToken Der "Bearer <TOKEN>" String.
     * @param statusId Die ID des zu löschenden Status.
     * @return Der gelöschte MastodonStatus als Bestätigung.
     */
    @DELETE
    @Path("/statuses/{id}")
    MastodonStatus deleteStatus(
            @HeaderParam("Authorization") String accessToken,
            @PathParam("id") String statusId
    );






    // Record für das Senden eines neuen Status
    record StatusPayload(String status, String visibility, String language) {}

    // Record, um die Antwort von /verify_credentials abzubilden
    // Wir brauchen hier nur die ID.
    record MastodonAccount(String id, String username) {}

    // Record, um einen einzelnen empfangenen Status abzubilden
    // @JsonProperty wird verwendet, um JSON-Felder (snake_case) auf Java-Felder (camelCase) zu mappen.
    record MastodonStatus(
            String id,
            @JsonProperty("created_at") ZonedDateTime createdAt,// Jackson wandelt den String automatisch in ein Datum um
            String content,
            MastodonAccount account
    ) {}
}
