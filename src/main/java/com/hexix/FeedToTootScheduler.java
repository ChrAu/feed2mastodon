package com.hexix;

import com.rometools.rome.feed.synd.SyndEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class FeedToTootScheduler {

    final Logger LOG = Logger.getLogger(this.getClass());
    @Inject
    FeedReaderService feedReader;

    @Inject
    @RestClient
    MastodonClient mastodonClient;

    @ConfigProperty(name = "mastodon.access.token")
    String accessToken;

    // Einfache In-Memory-Lösung zur Vermeidung von Duplikaten.
    // Für eine robuste Lösung eine Datei oder DB verwenden!
    private final Set<String> postedEntryGuids = ConcurrentHashMap.newKeySet();

//    @Scheduled(every = "10m") // Alle 10 Minuten ausführen
    void checkFeedAndPost() {
        postedEntryGuids.clear();
        System.out.println("Prüfe Feed auf neue Einträge...");
        List<SyndEntry> entries = feedReader.readFeedEntries();

        // Einträge sind oft von neu nach alt sortiert, also kehren wir die Liste um
        java.util.Collections.reverse(entries);

        for (SyndEntry entry : entries) {
            // Eine eindeutige ID pro Eintrag (GUID ist ideal, Link als Fallback)
            String entryId = entry.getUri() != null ? entry.getUri() : entry.getLink();

            if (!postedEntryGuids.contains(entryId)) {
                // Neuer Eintrag gefunden!
                System.out.println("Neuer Eintrag gefunden: " + entry.getTitle());

                // Toot-Text formatieren
                String tootText = entry.getTitle() + "\n\n" + entry.getLink();
                if (tootText.length() > 500) { // Mastodon-Zeichenlimit beachten
                    tootText = tootText.substring(0, 497) + "...";
                }

                try {
                    // An Mastodon senden
                    mastodonClient.postStatus("Bearer " + accessToken, new MastodonClient.StatusPayload(tootText));
                    System.out.println("Erfolgreich getootet: " + entry.getTitle());

                    // ID als "gepostet" markieren
                    postedEntryGuids.add(entryId);
                } catch (Exception e) {

                    LOG.error("Fehler beim Posten auf Mastodon: " + e.getMessage(), e);
                }
            }
        }
    }
}
