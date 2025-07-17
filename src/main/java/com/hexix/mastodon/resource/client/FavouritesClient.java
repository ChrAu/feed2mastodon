package com.hexix.mastodon.resource.client;

import com.hexix.mastodon.api.MastodonDtos;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/api/v1")
@RegisterRestClient(baseUri = "https://mastodon.hexix.de")
public interface FavouritesClient {

    @GET
    @Path("/favourites")
    @Produces(MediaType.APPLICATION_JSON)
    List<MastodonDtos.MastodonStatus> favourites(@HeaderParam("Authorization") String accessToken);

    @GET
    @Path("/favourites")
    @Produces(MediaType.APPLICATION_JSON)
    Response favourites2(
            @HeaderParam("Authorization") String accessToken,
            @QueryParam("max_id") String maxId, // Für "next"
            @QueryParam("min_id") String minId,  // Für "prev"
            @QueryParam("since_id") String sinceId,
            @QueryParam("limit") String limit
    );

}
