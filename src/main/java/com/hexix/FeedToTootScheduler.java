package com.hexix;

import com.hexix.ai.GeminiRequestEntity;
import com.hexix.ai.GenerateEmbeddingTextInput;
import com.hexix.ai.GenerateTextFromTextInput;
import com.hexix.mastodon.StarredMastodonPosts;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.MastodonClient;
import com.rometools.rome.feed.synd.SyndEntry;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    @ConfigProperty(name = "gemini.model")
    String geminiModel;



    // Einfache In-Memory-Lösung zur Vermeidung von Duplikaten.
    // Für eine robuste Lösung eine Datei oder DB verwenden!

    @Scheduled(every = "10m")
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
                    LOG.info("Neuer Eintrag gefunden in " + feed.feedUrl.substring(0, 25) + ": " + entry.getTitle());

                    MastodonDtos.StatusPayload statusPayload = new MastodonDtos.StatusPayload(getTootText(feed, entry, false), "unlisted", "de");;
                    PostedEntry newDbEntry = new PostedEntry();
                    if(feed.tryAi != null && feed.tryAi) {
                        final long countGeminiRequests = GeminiRequestEntity.countLast10Minutes(geminiModel);

                        if(countGeminiRequests > 3){
                            continue;
                        }


                        try {
                            String aiToot = generateTextFromTextInput.getAiMessage(geminiModel, getTootText(feed, entry, true));

                            if(aiToot.length() > 10 && aiToot.length() < 500){
                                statusPayload = new MastodonDtos.StatusPayload(aiToot, "public", "de");
                                newDbEntry.aiToot = true;
                            }
                        }catch (Exception e){
                            LOG.error("Beim generieren einer KI Nachricht ist ein Fehler aufgetreten", e);
                        }
                    }

                    try {
                        // An Mastodon senden
                        if(!feed.isActive){
                            continue;
                        }
                        newDbEntry.feed = feed;
                        newDbEntry.entryGuid = entryGuid;
                        newDbEntry.postedAt = Instant.now();
                        postAndPersist( statusPayload, newDbEntry);
                    } catch (Exception e) {
                        LOG.error("Fehler beim Posten auf Mastodon für Feed " + feed.feedUrl + ": " + e.getMessage(), e);
                        // Hier wird die Schleife fortgesetzt, um andere Einträge/Feeds nicht zu blockieren
                    }
                }else{
                    LOG.info("Eintrag bereits gepostet: " + entry.getTitle() + " - " + feed.feedUrl.substring(0, 25) + " -");
                }
            }
        }
        LOG.info("Job beendet.");

//        LOG.info("Prüfe Feed auf neue Einträge...");
//        List<SyndEntry> entries = feedReader.readFeedEntries();
//
//        // Einträge sind oft von neu nach alt sortiert, also kehren wir die Liste um
//        java.util.Collections.reverse(entries);
//
//        for (SyndEntry entry : entries) {
//            // Eine eindeutige ID pro Eintrag (GUID ist ideal, Link als Fallback)
//            String entryId = entry.getUri() != null ? entry.getUri() : entry.getLink();
//
//            if (!postedEntryGuids.contains(entryId)) {
//                // Neuer Eintrag gefunden!
//                LOG.info("Neuer Eintrag gefunden: " + entry.getTitle());
//
//                // Toot-Text formatieren
//                String tootText = entry.getTitle() + "\n\n" + entry.getLink();
//                if (tootText.length() > 500) { // Mastodon-Zeichenlimit beachten
//                    tootText = tootText.substring(0, 497) + "...";
//                }
//
//                try {
//                    // An Mastodon senden
//                    mastodonClient.postStatus("Bearer " + accessToken, new MastodonClient.StatusPayload(tootText));
//                    LOG.info("Erfolgreich getootet: " + entry.getTitle());
//
//                    // ID als "gepostet" markieren
//                    postedEntryGuids.add(entryId);
//                } catch (Exception e) {
//
//                    LOG.error("Fehler beim Posten auf Mastodon: " + e.getMessage(), e);
//                }
//            }
//        }
    }

    @Transactional
    void postAndPersist(final MastodonDtos.StatusPayload statusPayload, final PostedEntry newDbEntry) {
        MastodonDtos.MastodonStatus postedStatus = mastodonClient.postStatus("Bearer " + accessToken, statusPayload);

        // 4. Den neuen Eintrag in der Datenbank speichern
        newDbEntry.mastodonStatusId = postedStatus.id();

        newDbEntry.persist(); // Speichern!

        LOG.info("Erfolgreich getootet und in DB gespeichert. Status-ID: " + postedStatus.id());
    }

    private static String getTootText(final MonitoredFeed feed, final SyndEntry entry, boolean fullContent) {
        StringBuilder prefixText = new StringBuilder();
        if(feed.title != null && !feed.title.isEmpty()){
            prefixText.append(feed.title);
        }
        if(feed.defaultText != null && !feed.defaultText.isEmpty()){
            prefixText.append(feed.defaultText);
        }

        prefixText.append(entry.getTitle());
        if(entry.getDescription() != null && !entry.getDescription().getValue().isEmpty()) {
            prefixText.append("\n\n");
            prefixText.append(entry.getDescription().getValue());
        }else if(!entry.getContents().isEmpty() && entry.getContents().getFirst().getValue() != null && !entry.getContents().getFirst().getValue().isEmpty()){
            prefixText.append("\n\n");
            prefixText.append(entry.getContents().getFirst().getValue());
        }

        String link = "\n\n" + entry.getLink();

        if(prefixText.length() + link.length() > (fullContent ?25500 : 500)){
            prefixText = new StringBuilder(prefixText.substring(0, (fullContent ?25500 :497 - link.length())) + "...");
        }

        String tootText = prefixText +  link;
        return tootText;
    }



    @Scheduled(every = "10m")
    void checkMastodonStarred() {
        starredMastodonPosts.collectNewStarredPosts();
    }

    @Scheduled(every = "10m")
    void calcEmbeddings() {

        starredMastodonPosts.generateEmbeddings();

    }
}
