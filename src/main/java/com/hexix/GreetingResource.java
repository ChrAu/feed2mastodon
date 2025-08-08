package com.hexix;

import com.hexix.mastodon.Embedding;
import com.hexix.mastodon.PublicMastodonPostEntity;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.MastodonClient;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Path("/status")
public class GreetingResource {
    final static org.jboss.logging.Logger LOG = Logger.getLogger(JsoupParser.class);



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

    @GET@Path("/embeddings/loadURL")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Embedding> loadEmbeddingUrls(){
        return callEmbeddings(this::saveEmbeddingURL);
    }

    @GET@Path("/embeddings/loadText")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Embedding> loadEmbeddingText(){
        return callEmbeddings(this::saveEmbeddingText);
    }

    @GET@Path("/public/loadText")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Embedding> loadPublicText(){
        return callPublicMastodonPosts(this::savePublicText);
    }

    @Transactional
    void savePublicText(String mastodonId, MastodonDtos.MastodonStatus mastodonStatus) {
        StringJoiner sj = new StringJoiner("\n\n");


        final List<String> urls = MastodonDtos.MastodonStatus.extractLinksFromHtml(mastodonStatus.content());

        for (String url : urls) {
            // Annahme: JsoupParser.getArticle ist synchron und blockierend.
            // Wenn dies auch asynchron sein sollte, müsste es ebenfalls in ein Uni gewickelt werden.
            final String article = JsoupParser.getArticle(url);
            sj.add(article            );
        }


        final PublicMastodonPostEntity post = PublicMastodonPostEntity.findByMastodonId(mastodonId);
        post.setPostText(Jsoup.parse(mastodonStatus.content()).text());
        if(!sj.toString().isBlank()){
            post.setUrlText(sj.toString());
        }
    }


    @Transactional
    void saveEmbeddingText(String uuid, MastodonDtos.MastodonStatus mastodonStatus) {
        final Embedding embedding = Embedding.findByUUID(uuid);


        if(embedding.getResource().endsWith("__ONLY_TEXT")){
            embedding.setText(Jsoup.parse(mastodonStatus.content()).text());
        }else if(embedding.getResource().endsWith("__CARD_URL")){
            embedding.setText(JsoupParser.getArticle(embedding.getUrl()));
        }
    }


    private List<Embedding> callPublicMastodonPosts(BiConsumer<String, MastodonDtos.MastodonStatus> save){



        final List<PublicMastodonPostEntity[]> listOfPublicMastodonPosts = convertListToArrays(PublicMastodonPostEntity.<PublicMastodonPostEntity>findAll().list(), 20);


        for (PublicMastodonPostEntity[] publicMastodonPostEntities : listOfPublicMastodonPosts) {
            Map<String, List<String>> uuidStatusIdMap = new HashMap<>();
            for (PublicMastodonPostEntity publicMastodonPostEntity : publicMastodonPostEntities) {
                uuidStatusIdMap.computeIfAbsent(publicMastodonPostEntity.getMastodonId(), k -> new ArrayList<>()).add(publicMastodonPostEntity.getMastodonId());
            }

            final List<String> array = Arrays.stream(publicMastodonPostEntities).map(PublicMastodonPostEntity::getMastodonId).distinct().toList();


            final List<MastodonDtos.MastodonStatus> statuses = mastodonClient.getStatuses(array, "Bearer " + accessToken);

            for (MastodonDtos.MastodonStatus status : statuses) {
                final List<String> uuids = uuidStatusIdMap.get(status.id());

                for (String uuid : uuids) {

                    save.accept(uuid, status);
                }

            }

        }


        return Embedding.<Embedding>findAll().list();

    }


    private List<Embedding> callEmbeddings(BiConsumer<String, MastodonDtos.MastodonStatus> save){



        final List<Embedding[]> listOfEmbeddings = convertListToArrays(Embedding.<Embedding>findAll().list(), 20);


        for (Embedding[] embeddings : listOfEmbeddings) {
            Map<String, List<String>> uuidStatusIdMap = new HashMap<>();
            for (Embedding embedding : embeddings) {
                uuidStatusIdMap.computeIfAbsent(embedding.getMastodonStatusId(), k -> new ArrayList<>()).add(embedding.getUuid());
            }

            final List<String> array = Arrays.stream(embeddings).map(Embedding::getMastodonStatusId).distinct().toList();


            final List<MastodonDtos.MastodonStatus> statuses = mastodonClient.getStatuses(array, "Bearer " + accessToken);

            for (MastodonDtos.MastodonStatus status : statuses) {
                final List<String> uuids = uuidStatusIdMap.get(status.id());

                for (String uuid : uuids) {

                    save.accept(uuid, status);
                }

            }

        }


        return Embedding.<Embedding>findAll().list();

    }

    @Transactional
    void saveEmbeddingURL(final String uuid, final MastodonDtos.MastodonStatus status) {
        Embedding.findByUUID(uuid).setStatusOriginalUrl(status.url());
    }


    /**
     * Wandelt eine Liste von Strings in eine Liste von String-Arrays um,
     * wobei jedes Array eine bestimmte maximale Größe hat.
     *
     * @param originalList Die ursprüngliche Liste von Strings.
     * @param chunkSize    Die gewünschte Größe jedes String-Arrays.
     * @return Eine Liste von String-Arrays.
     */
    public static <E>List<E[]> convertListToArrays(List<E> originalList, int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize muss größer als 0 sein.");
        }

        List<E[]> result = new ArrayList<>();
        int listSize = originalList.size();

        for (int i = 0; i < listSize; i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, listSize);
            List<E> subList = originalList.subList(i, endIndex);
            result.add(subList.toArray((E[]) Array.newInstance(originalList.get(0).getClass(), 0))); // Umwandlung der Sub-Liste in ein Array
        }
        return result;
    }

    @POST
    @Path("/negativ/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response receiveIdFromBody(IdPayload idPayload) {

        final PublicMastodonPostEntity post = PublicMastodonPostEntity.findByMastodonId(idPayload.getId());
        post.setNegativeWeight(1.0);

        mastodonClient.unBoostStatus(idPayload.getId(), "Bearer " + accessToken);

        return Response.ok().build();
    }

    /**
     * Eine Hilfsklasse (POJO), um den JSON-Body zu binden.
     * Jackson oder JSON-B mappen den eingehenden JSON automatisch auf dieses Objekt.
     */
    public static class IdPayload {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
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
