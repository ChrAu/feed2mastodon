package com.hexix;

import com.hexix.ai.GenerateTextFromTextInput;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    @ConfigProperty(name = "mastodon.access.token")
    String accessToken;

    // Einfache In-Memory-Lösung zur Vermeidung von Duplikaten.
    // Für eine robuste Lösung eine Datei oder DB verwenden!

    @Scheduled(every = "10m")
        // Alle 10 Minuten ausführen
    @Transactional
    void checkFeedAndPost() {
        List<MonitoredFeed> activeFeeds = MonitoredFeed.list("isActive", true);

        for (MonitoredFeed feed : activeFeeds) {
            LOG.info("Verarbeite Feed: " + feed.feedUrl);
            List<SyndEntry> entriesFromFeed = feedReader.readFeedEntries(feed.feedUrl);

            entriesFromFeed = entriesFromFeed.stream().filter(syndEntry -> {
                final LocalDateTime feedPublishedLocalDateTime = syndEntry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                return feed.addDate.isBefore(feedPublishedLocalDateTime);
            }).collect(Collectors.toList());

            // Einträge umkehren, um sie in chronologischer Reihenfolge zu posten
            Collections.reverse(entriesFromFeed);

            for (SyndEntry entry : entriesFromFeed) {
                String entryGuid = entry.getUri() != null ? entry.getUri() : entry.getLink();

                // 2. Prüfe in der DB, ob dieser Eintrag für diesen Feed bereits gepostet wurde
                long count = PostedEntry.count("feed = ?1 and entryGuid = ?2", feed, entryGuid);

                if (count == 0) {
                    // 3. Neuer Eintrag! Posten und in der DB vermerken.
                    LOG.info("Neuer Eintrag gefunden in " + feed.feedUrl + ": " + entry.getTitle());

                    String tootText = getTootText(feed, entry);
                    MastodonClient.StatusPayload statusPayload = new MastodonClient.StatusPayload(tootText, "unlisted");
                    PostedEntry newDbEntry = new PostedEntry();
                    if(feed.tryAi != null && feed.tryAi) {
                        try {
                            String aiToot = generateTextFromTextInput.getAiMessage(tootText);

                            if(aiToot.length() > 10 && aiToot.length() < 500){
                                statusPayload = new MastodonClient.StatusPayload(aiToot, "public");
                                newDbEntry.aiToot = true;
                            }
                        }catch (Exception e){
                            LOG.error("Beim generieren einer KI Nachricht ist ein Fehler aufgetreten", e);
                        }
                    }

                    try {
                        // An Mastodon senden
                        MastodonClient.MastodonStatus postedStatus = mastodonClient.postStatus("Bearer " + accessToken,statusPayload);

                        // 4. Den neuen Eintrag in der Datenbank speichern
                        newDbEntry.feed = feed;
                        newDbEntry.entryGuid = entryGuid;
                        newDbEntry.mastodonStatusId = postedStatus.id();
                        newDbEntry.postedAt = Instant.now();
                        newDbEntry.persist(); // Speichern!

                        LOG.info("Erfolgreich getootet und in DB gespeichert. Status-ID: " + postedStatus.id());
                    } catch (Exception e) {
                        System.err.println("Fehler beim Posten auf Mastodon für Feed " + feed.feedUrl + ": " + e.getMessage());
                        // Hier wird die Schleife fortgesetzt, um andere Einträge/Feeds nicht zu blockieren
                    }
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

    private static String getTootText(final MonitoredFeed feed, final SyndEntry entry) {
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
        }

        String link = "\n\n" + entry.getLink();

        if(prefixText.length() + link.length() > 500){
            prefixText = new StringBuilder(prefixText.substring(0, (497 - link.length())) + "...");
        }

        String tootText = prefixText +  link;
        return tootText;
    }
}
