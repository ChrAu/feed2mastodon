package com.hexix.mastodon.resource;

import com.hexix.JsoupParser;
import com.hexix.ai.GenerateEmbeddingTextInput;
import com.hexix.mastodon.Embedding;
import com.hexix.mastodon.PagingConfigEntity;
import com.hexix.mastodon.TextEntity;
import com.hexix.mastodon.TextEntityRepository;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.client.FavouritesClient;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequestScoped
public class FavouritesService {

    final Logger LOG = Logger.getLogger(this.getClass());


    @Inject
    @RestClient
    FavouritesClient favouritesClient;

    @Inject
    GenerateEmbeddingTextInput generateEmbeddingTextInput;

    @ConfigProperty(name = "mastodon.private.access.token")
    String privateAccessToken;
    @Inject
    TextEntityRepository textEntityRepository;

    public List<MastodonDtos.MastodonStatus> getAllFavourites() {
        return getNewFavourites();
    }

    @Transactional
    public List<MastodonDtos.MastodonStatus> getNewFavourites() {

        PagingConfigEntity favourites = PagingConfigEntity.find("favourites");
        if(favourites == null) {
            favourites = new PagingConfigEntity("favourites");
            favourites.persist();
        }

        List<MastodonDtos.MastodonStatus> results = new LinkedList<>();
        String minId = null;
        String maxId = null;
        String oldMaxId = null;

        String sinceId = favourites.getSinceId();

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


                final Link prev = response.getLink("prev");
                if(prev != null){
                    final String queryParams = prev.getUri().getQuery();

                    String min = parseLink(queryParams, "min_id");

                    if(min != null && (favourites.getSinceId() == null || Integer.parseInt(favourites.getSinceId()) < Integer.parseInt(min))) {
                        favourites.setSinceId(min);
                    }


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

    @Transactional
    public void createEmbedding(final MastodonDtos.MastodonStatus mastodonStatus) {

        final String content = mastodonStatus.content();
        final String onlyText = Jsoup.parse(content).text();
        Map<String, MastodonText> list = new HashMap<>();

        if(onlyText != null && !onlyText.trim().isEmpty()) {
            list.put("MASTODON_STATUS_ID__"+ mastodonStatus.id() + "__ONLY_TEXT", new MastodonText(onlyText, null, mastodonStatus.url()));
        }

        final MastodonDtos.PreviewCard card = mastodonStatus.card();
        if(card != null) {
            final String article = JsoupParser.getArticle(card.url());
            if(article != null && !article.trim().isEmpty()) {
                list.put("MASTODON_STATUS_ID__" + mastodonStatus.id() + "__CARD_URL", new MastodonText(article, card.url(), mastodonStatus.url()));
            }else{
                list.put("MASTODON_STATUS_ID__" + mastodonStatus.id() + "__CARD_URL", new MastodonText(null, card.url(), mastodonStatus.url()));
            }
        }


//        final Map<String, ContentEmbedding> embedding = generateEmbeddingTextInput.getEmbedding("gemini-embedding-001", list);


        list.forEach((key, value) -> {
            final Embedding embedding = new Embedding();
            final TextEntity textEntity = new TextEntity(value.text);
            if(textEntity.getText() != null && !textEntity.getText().isBlank()){
                textEntityRepository.persist(textEntity);
                embedding.setText(textEntity);
            }

            embedding.setMastodonStatusId(mastodonStatus.id());
            embedding.setResource(key);
            embedding.setUrl(value.url);
            embedding.persist();
        });


    }

    record MastodonText(String text, String url, String statusUrl){}
}
