package com.hexix.mastodon.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hexix.mastodon.api.MastodonDtos;
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
    MastodonDtos.MastodonStatus postStatus(@HeaderParam("Authorization") String accessToken, MastodonDtos.StatusPayload status);

    /**
     * Ruft die Account-Informationen ab, die mit dem Access Token verknüpft sind.
     * Nützlich, um die eigene Account-ID zu erhalten.
     * @param accessToken Der "Bearer <TOKEN>" String.
     * @return Ein MastodonAccount-Objekt.
     */
    @GET
    @Path("/accounts/verify_credentials")
    MastodonDtos.MastodonAccount verifyCredentials(@HeaderParam("Authorization") String accessToken);

    /**
     * Ruft eine Liste der Status-Updates für eine gegebene Account-ID ab.
     * @param accessToken Der "Bearer <TOKEN>" String.
     * @param accountId Die ID des Accounts, dessen Posts abgerufen werden sollen.
     * @param limit Die maximale Anzahl an Posts, die zurückgegeben werden soll (optional).
     * @return Eine Liste von MastodonStatus-Objekten.
     */
    @GET
    @Path("/accounts/{id}/statuses")
    List<MastodonDtos.MastodonStatus> getAccountStatuses(
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
    MastodonDtos.MastodonStatus deleteStatus(
            @HeaderParam("Authorization") String accessToken,
            @PathParam("id") String statusId
    );







}
