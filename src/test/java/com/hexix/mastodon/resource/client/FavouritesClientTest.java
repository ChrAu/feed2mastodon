package com.hexix.mastodon.resource.client;

import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.FavouritesResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class FavouritesClientTest {

    @Inject
    @RestClient
    FavouritesClient favouritesClient;

    @Inject
    FavouritesResource favouritesResource;

    @ConfigProperty(name ="mastodon.private.access.token")
    String privateAccessToken;



    @Test
    public void favouritesClient(){
        final List<MastodonDtos.MastodonStatus> favourites = favouritesClient.favourites("Bearer " + privateAccessToken);

        System.out.println(favourites);
    }

    @Test
    public void setFavouritesResource(){
        final List<MastodonDtos.MastodonStatus> favourites = favouritesResource.getNewFavourites("566");

        System.out.println(favourites);
    }

}
