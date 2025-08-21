package com.hexix;

import com.hexix.ai.GeminiRequestEntity;
import com.hexix.ai.GenerateEmbeddingTextInput;
import com.hexix.ai.GenerateTextFromTextInput;
import com.hexix.ai.OllamaRestClient;
import com.hexix.ai.dto.EmbeddingRequest;
import com.hexix.ai.dto.EmbeddingResponse;
import com.hexix.mastodon.Embedding;
import com.hexix.mastodon.PublicMastodonPostEntity;
import com.hexix.mastodon.StarredMastodonPosts;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.MastodonClient;
import com.hexix.util.VektorUtil;
import com.rometools.rome.feed.synd.SyndEntry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class FeedToTootScheduler {

    final Logger LOG = Logger.getLogger(this.getClass());
    @Inject
    FeedReaderService feedReader;

    @Inject
    @RestClient
    MastodonClient mastodonClient;

    @Inject
    GenerateTextFromTextInput generateTextFromTextInput;

    @Inject
    StarredMastodonPosts starredMastodonPosts;

    @ConfigProperty(name = "mastodon.access.token")
    String accessToken;

    @ConfigProperty(name = "mastodon.private.access.token")
    String privateAccessToken;

    @ConfigProperty(name = "gemini.model")
    String geminiModel;

    @ConfigProperty(name = "local.model", defaultValue = "granite-embedding:278m")
    String localModel;

    @ConfigProperty(name = "mastodon.boost.disable", defaultValue = "true")
    Boolean boostDisable;

    @ConfigProperty(name = "feed2Mastodon.minCosDistance", defaultValue = "0.825")
    Double minCosDistance;


    @Inject
    @RestClient
    OllamaRestClient ollamaRestClient;
    @Inject
    GenerateEmbeddingTextInput generateEmbeddingTextInput;


    // Einfache In-Memory-Lösung zur Vermeidung von Duplikaten.
    // Für eine robuste Lösung eine Datei oder DB verwenden!

    private static String getTootText(final MonitoredFeed feed, final SyndEntry entry, boolean fullContent) {
        StringBuilder prefixText = new StringBuilder();
        if (feed.title != null && !feed.title.isEmpty()) {
            prefixText.append(feed.title);
        }
        if (feed.defaultText != null && !feed.defaultText.isEmpty()) {
            prefixText.append(feed.defaultText);
        }

        prefixText.append(entry.getTitle());
        if (entry.getDescription() != null && !entry.getDescription().getValue().isEmpty()) {
            prefixText.append("\n\n");
            prefixText.append(entry.getDescription().getValue());
        } else if (!entry.getContents().isEmpty() && entry.getContents().getFirst().getValue() != null && !entry.getContents().getFirst().getValue().isEmpty()) {
            prefixText.append("\n\n");
            prefixText.append(entry.getContents().getFirst().getValue());
        }

        String link = "\n\n" + entry.getLink();

        if (prefixText.length() + link.length() > (fullContent ? 25500 : 500)) {
            prefixText = new StringBuilder(prefixText.substring(0, (fullContent ? 25500 : 497 - link.length())) + "...");
        }

        String tootText = prefixText + link;
        return tootText;
    }

    @Scheduled(every = "10m",delay = 30, delayUnit = TimeUnit.SECONDS,  concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
        // Alle 10 Minuten ausführen
//    @Transactional
    void checkFeedAndPost() {
        List<MonitoredFeed> activeFeeds = MonitoredFeed.findAll().list();

        for (MonitoredFeed feed : activeFeeds) {
            LOG.info("Verarbeite Feed: " + feed.feedUrl);
            List<SyndEntry> entriesFromFeed = feedReader.readFeedEntries(feed.feedUrl);

            entriesFromFeed = entriesFromFeed.stream().filter(syndEntry -> {


                final LocalDateTime feedPublishedLocalDateTime = Optional.ofNullable(syndEntry.getPublishedDate()).orElse(syndEntry.getUpdatedDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                return feed.addDate.isBefore(feedPublishedLocalDateTime);
            }).sorted(Comparator.comparing(syndEntry -> Optional.ofNullable(syndEntry.getPublishedDate()).orElse(syndEntry.getUpdatedDate()), Comparator.nullsLast(Comparator.naturalOrder()))).toList();

            // Einträge umkehren, um sie in chronologischer Reihenfolge zu posten
//            Collections.reverse(entriesFromFeed);

            for (SyndEntry entry : entriesFromFeed) {

                String entryGuid = entry.getUri() != null ? entry.getUri() : entry.getLink();

                // 2. Prüfe in der DB, ob dieser Eintrag für diesen Feed bereits gepostet wurde
                long count = PostedEntry.count("feed = ?1 and entryGuid = ?2", feed, entryGuid);

                if (count == 0) {
                    // 3. Neuer Eintrag! Posten und in der DB vermerken.
                    LOG.debug("Neuer Eintrag gefunden in " + feed.feedUrl.substring(0, 25) + ": " + entry.getTitle());

                    MastodonDtos.StatusPayload statusPayload = new MastodonDtos.StatusPayload(getTootText(feed, entry, false), "unlisted", "de");
                    PostedEntry newDbEntry = new PostedEntry();
                    if (feed.tryAi != null && feed.tryAi) {
                        final long countGeminiRequests = GeminiRequestEntity.countLast10Minutes(geminiModel);

                        if (countGeminiRequests > 3) {
                            continue;
                        }


                        try {
                            String aiToot = generateTextFromTextInput.getAiMessage(geminiModel, getTootText(feed, entry, true));

                            int maxLength = 500;

                            if(entry.getUri().length()< 25){
                                maxLength = maxLength + (entry.getUri().length() - 25);
                            }


                            if (aiToot.length() > 10 && aiToot.length() < maxLength) {
                                statusPayload = new MastodonDtos.StatusPayload(aiToot, "public", "de");
                                newDbEntry.aiToot = true;
                            }
                        } catch (Exception e) {
                            LOG.error("Beim generieren einer KI Nachricht ist ein Fehler aufgetreten", e);
                        }
                    }

                    try {
                        // An Mastodon senden
                        if (!feed.isActive) {
                            continue;
                        }
                        newDbEntry.feed = feed;
                        newDbEntry.entryGuid = entryGuid;
                        newDbEntry.postedAt = Instant.now();
                        postAndPersist(statusPayload, newDbEntry);
                    } catch (Exception e) {
                        LOG.error("Fehler beim Posten auf Mastodon für Feed " + feed.feedUrl + ": " + e.getMessage(), e);
                        // Hier wird die Schleife fortgesetzt, um andere Einträge/Feeds nicht zu blockieren
                    }
                } else {
                    LOG.debug("Eintrag bereits gepostet: " + entry.getTitle() + " - " + feed.feedUrl.substring(0, 25) + " -");
                }
            }
        }
        LOG.info("Job beendet.");

    }

    @Transactional
    void postAndPersist(final MastodonDtos.StatusPayload statusPayload, final PostedEntry newDbEntry) {
        MastodonDtos.MastodonStatus postedStatus = mastodonClient.postStatus("Bearer " + accessToken, statusPayload);

        // 4. Den neuen Eintrag in der Datenbank speichern
        newDbEntry.mastodonStatusId = postedStatus.id();

        newDbEntry.persist(); // Speichern!

        LOG.info("Erfolgreich getootet und in DB gespeichert. Status-ID: " + postedStatus.id());
    }

    @Scheduled(every = "300s",delay = 30, delayUnit = TimeUnit.SECONDS, concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void checkMastodonStarred() {
        starredMastodonPosts.collectNewStarredPosts();
    }

    @Scheduled(every = "10s", delay = 30, delayUnit = TimeUnit.SECONDS, concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void calcEmbeddings() {
        starredMastodonPosts.generateEmbeddings();
        starredMastodonPosts.generateLocalEmbeddings();
    }

    @Transactional
    @Scheduled(every = "24h", delay = 60, delayUnit = TimeUnit.SECONDS, concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void calcEmbeddingsArticles() {


        final List<Embedding> embeddings = Embedding.<Embedding>find("text is null").list();

        embeddings.stream().filter(embedding -> embedding.getText() ==null).filter(embedding -> embedding.getUrl() != null).forEach(embedding -> embedding.setText(JsoupParser.getArticle(embedding.getUrl())));
    }


    @Transactional
    Map<String, List<EmbeddingRequest>> generateOllamaRequest() {

        Map<String, List<EmbeddingRequest>> allRequests = new HashMap<>();
        final List<PublicMastodonPostEntity> nextPublicMastodonPost = PublicMastodonPostEntity.findNextPublicMastodonPost();

        for (PublicMastodonPostEntity post : nextPublicMastodonPost) {

            List<EmbeddingRequest> requests = new ArrayList<>();
            if(post.getPostText() != null && !post.getPostText().isBlank()) {
                requests.add(new EmbeddingRequest(localModel, List.of(post.getPostText()), true));
            }

            final String urlText = post.getUrlText();
            if (urlText != null && !urlText.isBlank()) {

                final List<String> texte = StarredMastodonPosts.splitByLength(urlText, "bge-m3:567m".equalsIgnoreCase(localModel) ? 8000 : 500);
                for (String subText : texte) {
                    if(subText.isBlank()){
                        continue;
                    }
                    final EmbeddingRequest requestUrl = new EmbeddingRequest(localModel, List.of(subText), false);


                    requests.add(requestUrl);

                }

            }

            allRequests.put(post.getMastodonId(), requests);
        }

        return allRequests;
    }

    @Scheduled(every = "10s", delay = 30, delayUnit = TimeUnit.SECONDS,  concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void calcPublicVectors() {
        int calcRequests = 0;
        final Map<String, List<EmbeddingRequest>> requests = generateOllamaRequest();

        if(!requests.isEmpty()){
            LOG.infof("Generiere für folgende Einträge Vektoren: %s", requests.keySet());
        }


        for (Map.Entry<String, List<EmbeddingRequest>> entry : requests.entrySet()) {
            try {
                final String mastodonId = entry.getKey();
                final List<EmbeddingRequest> embeddingRequests = entry.getValue();
                List<double[]> vectors = new ArrayList<>();

                for (EmbeddingRequest request : embeddingRequests) {
                    final EmbeddingResponse postResponse = ollamaRestClient.generateEmbeddings(request);
                    calcRequests++;
                    vectors.add(postResponse.embeddings().getFirst().stream().mapToDouble(Double::doubleValue).toArray());
                }

                final double[] profileVector = VektorUtil.createProfileVector(vectors);

                savePublicVector(mastodonId, profileVector);
            } catch (Exception e) {

                LOG.errorf(e, "Fehler beim Vektor generieren für ID: %s", entry.getKey());
            }


        }
        if (calcRequests > 0) {
            LOG.infof("Es wurden %s Ollama Vektoren berechnet", calcRequests);
        }
    }

    @Transactional
    void savePublicVector(final String mastodonId, final double[] profileVector) {
        final PublicMastodonPostEntity mastodonPost = PublicMastodonPostEntity.<PublicMastodonPostEntity>find("mastodonId = ?1", mastodonId).firstResult();
        mastodonPost.setEmbeddingVector(profileVector);
        mastodonPost.setEmbeddingModel(localModel);
        LOG.debugf("Speichere Vektor für Id: %s", mastodonPost.getMastodonId());
    }



    @Scheduled(every = "10s", delay = 30, delayUnit = TimeUnit.SECONDS,  concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void fetchPublicText() {
        final List<PublicMastodonPostEntity> posts = PublicMastodonPostEntity.findAllNoEmbeddingAndText();

        if(posts.isEmpty()){
            return;
        }
        final List<MastodonDtos.MastodonStatus> statuses = mastodonClient.getStatuses(posts.stream().map(PublicMastodonPostEntity::getMastodonId).toList(), "Bearer " + accessToken);
        Map<String, MastodonDtos.MastodonStatus> statusMap = new HashMap<>();
        for (MastodonDtos.MastodonStatus status : statuses) {
            statusMap.put(status.id(), status);
        }


        for (PublicMastodonPostEntity post : posts) {
            try {

                readStatusAndLinkText(post, statusMap.get(post.getMastodonId()));
            } catch (Exception e) {
                LOG.errorf(e, "Fehler beim bearbeiten des Posts mit Id: %s", post.getMastodonId());
            }
        }

    }
    @Transactional
    void readStatusAndLinkText(final PublicMastodonPostEntity p, final MastodonDtos.MastodonStatus mastodonStatus) {
        final PublicMastodonPostEntity post = PublicMastodonPostEntity.findById(p.id);
        try {
            if(p.getPostText() == null && mastodonStatus != null){
                post.setPostText(Jsoup.parse(mastodonStatus.content()).text());
            }


            if (post.getPostText() == null || post.getPostText().isBlank() || post.getPostText().isEmpty()) {
                if(post.getNegativeWeight() == null) {
                    post.delete();
                    return;
                }
            }

            final Boolean noURL = post.isNoURL();
            if (noURL == null || !noURL) {

                final List<String> urls = MastodonDtos.MastodonStatus.extractLinksFromHtml(post.getPostText());

                if (!urls.isEmpty()) {
                    StringJoiner sj = new StringJoiner("\n\n");
                    for (String url : urls) {
                        // Annahme: JsoupParser.getArticle ist synchron und blockierend.
                        // Wenn dies auch asynchron sein sollte, müsste es ebenfalls in ein Uni gewickelt werden.
                        final String article = JsoupParser.getArticle(url);
                        if (article != null) {
                            sj.add(article);
                        }
                    }
                    if (sj.length() > 0) {
                        post.setUrlText(sj.toString());
                    }
                }
            }
        }catch (Exception e){
            post.setUrlText(null);
        }

    }

    @Transactional
    @Scheduled(every = "10s",delay = 30, delayUnit = TimeUnit.SECONDS,  concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void calcRecommendations() {

        final boolean embeddingsAllCalced = Embedding.findNextLocalEmbeddings().isEmpty();

        List<PublicMastodonPostEntity> posts = PublicMastodonPostEntity.findAllComparable();

        if(posts.isEmpty() && embeddingsAllCalced){
            return;
        }

        LOG.info("Schwelle für das Posten ist Distanz größer: " + minCosDistance);


        final List<Embedding> allLocalEmbeddings = Embedding.findAllLocalEmbeddings();

        final List<Embedding> positivList = allLocalEmbeddings.stream().filter(embedding -> embedding.getNegativeWeight() == null).toList();

        final List<VektorUtil.VektorWeight> positiveVektoren = new ArrayList<>(positivList.stream().map(Embedding::getLocalEmbedding).map(doubles -> new VektorUtil.VektorWeight(doubles, 1.0)).toList());
        final List<VektorUtil.VektorWeight> negativeVektoren = new ArrayList<>();

//        double[] originalVektor = VektorUtil.createProfileVector(positiveVektoren);

        Map<Double, List<Embedding>> negativeEmbeddings = new HashMap<>();

        allLocalEmbeddings.stream().filter(embedding -> embedding.getNegativeWeight() != null).forEach(embedding -> negativeEmbeddings.computeIfAbsent(embedding.getNegativeWeight(), k -> new ArrayList<>()).add(embedding));


        for (Map.Entry<Double, List<Embedding>> entry : negativeEmbeddings.entrySet()) {
            final double weight = entry.getKey();
            final List<Embedding> negativList = entry.getValue();
            final List<double[]> negativVektors = negativList.stream().map(Embedding::getLocalEmbedding).toList();
            negativeVektoren.addAll(negativVektors.stream().map(doubles -> new VektorUtil.VektorWeight(doubles, weight)).toList());

        }

        Map<Double, List<PublicMastodonPostEntity>> negativePosts = new HashMap<>();

        PublicMastodonPostEntity.findAllNegativPosts().forEach(post -> negativePosts.computeIfAbsent(post.getNegativeWeight(), k -> new ArrayList<>()).add(post));


        for (Map.Entry<Double, List<PublicMastodonPostEntity>> entry : negativePosts.entrySet()) {
            final double weight = entry.getKey();
            final List<PublicMastodonPostEntity> negativList = entry.getValue();
            final List<double[]> negativVektors = negativList.stream().map(PublicMastodonPostEntity::getEmbeddingVector).toList();
            negativeVektoren.addAll(negativVektors.stream().map(doubles -> new VektorUtil.VektorWeight(doubles, weight)).toList());
        }

        final double[] profileVector = VektorUtil.createProfileVector(positiveVektoren, negativeVektoren);




        for (PublicMastodonPostEntity post : posts) {
            final double[] embeddingVector = post.getEmbeddingVector();

            try{


                final double cosineSimilarity = VektorUtil.getCosineSimilarity(profileVector, embeddingVector);
                post.setCosDistance(cosineSimilarity);
            }catch (IllegalArgumentException e){
               LOG.error("Fehler mit PublicMastodonPostEntity: " + post.getMastodonId(), e);
            }
            if (post.getCosDistance() != null && post.getCosDistance() > minCosDistance) {
                if (boostDisable != null && !boostDisable) {
                    try {
                        final MastodonDtos.MastodonStatus mastodonStatus = mastodonClient.boostStatus(post.getMastodonId(), new MastodonDtos.BoostStatusRequest(MastodonDtos.MastodonStatus.StatusVisibility.PRIVATE), "Bearer " + accessToken);
                    } catch (Exception e) {
                        LOG.errorf(e, "Fehler beim Boosten der MastodonId: %s", post.getMastodonId());
                        post.setCosDistance(Double.NEGATIVE_INFINITY);
                        continue;
                    }
                }

                LOG.infof("Mastodon Satatus (ID: %s) wurde geboosted", post.getMastodonId());
            }
        }
    }


    @Scheduled(every = "P1D",delay = 120, concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void removeText() {

        LOG.info("Lösche alle alten Embedding Texte");
        final List<Embedding> allCalcedEmbeddings = Embedding.findAllCalcedEmbeddings();
//
        allCalcedEmbeddings.forEach(embedding -> embedding.setText(null));

        final List<PublicMastodonPostEntity> allComparable = PublicMastodonPostEntity.findAllCalcedEmbeddings();
        allComparable.forEach(post -> {
            post.setPostText(null);
            post.setUrlText(null);
        });

        LOG.info("Löschen abgeschlossen");
    }

}
