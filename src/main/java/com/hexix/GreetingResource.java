package com.hexix;

import com.hexix.mastodon.resource.MastodonClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/status")
public class GreetingResource {


    @Inject FeedToTootScheduler feedToTootScheduler;

    @Inject
    @RestClient
    MastodonClient mastodonClient;

    @ConfigProperty(name = "mastodon.access.token")
    String accessToken;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        feedToTootScheduler.checkFeedAndPost();
        return "Hello from Quarkus REST";
    }

//    @GET
//    @Path("/delete")
//    @Produces(MediaType.TEXT_PLAIN)
//    public String showStatuses(){
//        final MastodonClient.MastodonAccount account = mastodonClient.verifyCredentials("Bearer " + accessToken );
//        final List<MastodonClient.MastodonStatus> statuses = mastodonClient.getAccountStatuses("Bearer " + accessToken, account.id(), 10);
//
//        StringBuilder sb = new StringBuilder();
//        for (MastodonClient.MastodonStatus status : statuses) {
//            if(status.createdAt().isAfter(ZonedDateTime.of(2025, 6, 11, 0,0,0,0, ZoneId.of("Europe/Berlin")))){
//                final MastodonClient.MastodonStatus mastodonStatus = mastodonClient.deleteStatus("Bearer " + accessToken, status.id());
//                sb.append(status.content()).append("\n");
//            }
//
//
//        }
//        return sb.toString();
//    }
}
