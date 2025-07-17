package com.hexix.mastodon.resource;

import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.client.FavouritesClient;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequestScoped
public class FavouritesResource {

    final Logger LOG = Logger.getLogger(this.getClass());


    @Inject
    @RestClient
    FavouritesClient favouritesClient;

    @ConfigProperty(name = "mastodon.private.access.token")
    String privateAccessToken;

    public List<MastodonDtos.MastodonStatus> getAllFavourites() {
        return getNewFavourites(null);
    }

    public List<MastodonDtos.MastodonStatus> getNewFavourites(String sinceId) {

        List<MastodonDtos.MastodonStatus> results = new LinkedList<>();
        String minId = null;
        String maxId = null;
        String oldMaxId = null;
        do {
            oldMaxId = maxId;

            try (final Response response = favouritesClient.favourites2("Bearer " + privateAccessToken, maxId, minId, sinceId, "40")) {

                final List<MastodonDtos.MastodonStatus> mastodonStatuses = response.readEntity(new GenericType<>() {
                });

                final Link next = response.getLink("next");
                if(next != null) {
                    final String queryParams = next.getUri().getQuery();

                    maxId = parseLink(queryParams, "max_id");
                }

                results.addAll(0, mastodonStatuses);

            } catch (Exception e) {
                LOG.error("Beim anfragen der Favourite ist ein Fehler aufgetreten:", e);
                throw new RuntimeException(e);
            }

        }while (!Objects.equals(maxId, oldMaxId));


        return results;
    }

    /**
     * Eine einfache Hilfsmethode, um einen Wert aus dem Link-Header zu parsen.
     */
    private String parseLink(String link, String paramName) {
        if (link == null || !link.contains(paramName)) {
            return null;
        }
        // Einfacher Regex, um z.B. min_id=569 zu finden
        Pattern pattern = Pattern.compile(paramName + "=([^&>]+)");
        Matcher matcher = pattern.matcher(link);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
