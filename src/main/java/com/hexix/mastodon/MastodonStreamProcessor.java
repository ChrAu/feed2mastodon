package com.hexix.mastodon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexix.JsoupParser;
import com.hexix.ai.OllamaRestClient;
import com.hexix.ai.dto.EmbeddingRequest;
import com.hexix.ai.dto.EmbeddingResponse;
import com.hexix.mastodon.api.MastodonDtos;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Diese Bean abonniert den Mastodon Streaming API beim Start der Anwendung.
 * Sie verarbeitet die empfangenen Ereignisse und implementiert eine Wiederverbindungslogik.
 */
@ApplicationScoped
public class MastodonStreamProcessor {

    final static Logger LOG = Logger.getLogger(JsoupParser.class);
    // Injiziert den MastodonStreamingService für den Zugriff auf den Stream
    @Inject
    @RestClient
    MastodonStreamingService mastodonStreamingService;

    @Inject
    @RestClient
    OllamaRestClient ollamaRestClient;

    // Injiziert den Jackson ObjectMapper für die JSON-Deserialisierung
    @Inject
    ObjectMapper objectMapper;


    @ConfigProperty(name = "mastodon.private.access.token")
    String privateAccessToken;

    /**
     * Diese Methode wird beim Start der Quarkus-Anwendung ausgelöst.
     * Sie abonniert den Mastodon-Stream und richtet die Fehlerbehandlung ein.
     *
     * @param ev Das StartupEvent, das den Anwendungsstart signalisiert.
     */
    void onStart(@Observes StartupEvent ev) {
        LOG.info("Anwendung startet, abonniere Mastodon Stream...");
        subscribeToMastodonStream();
    }

    /**
     * Abonniert den Mastodon-Stream und definiert die Verarbeitungs- und Wiederverbindungslogik.
     */
    private void subscribeToMastodonStream() {
        final Multi<String> stringMulti = mastodonStreamingService.streamPublicTimeline("Bearer " + privateAccessToken);
        stringMulti
                // Wenn ein Fehler auftritt (z.B. Verbindungsabbruch), versuche die Verbindung erneut herzustellen.
                // retry().withBackoff(initialBackoff, maxBackoff): Versucht es erneut mit exponentiellem Backoff.
                // atMost(Long.MAX_VALUE): Versucht es unendlich oft.
                .onFailure(throwable -> {
                    Log.error("Fehler beim Stream-Empfang: " +throwable.getMessage() , throwable);
                    return false;
                }).retry().withBackOff(Duration.ofSeconds(5), Duration.ofSeconds(60)).atMost(Long.MAX_VALUE)
                .subscribe().with(
                        dataPayload -> {
                            // Erfolgreich empfangene Daten-Payload verarbeiten
                            processDataPayload(dataPayload);
                        },
                        failure -> {
                            // Fehler beim Stream-Empfang (nach allen Wiederholungsversuchen)
                            Log.error("Fehler im Mastodon Stream nach Wiederholungsversuchen: " + failure.getMessage(), failure);
                            // Hier könnten Sie weitere Aktionen auslösen, z.B. Benachrichtigungen.
                        }
                );
    }

    /**
     * Verarbeitet die empfangene Daten-Payload vom Mastodon-Stream.
     * Versucht, die Payload als MastodonStatus zu parsen oder als Delete-Ereignis zu identifizieren.
     *
     * @param dataPayload Der rohe String-Inhalt des "data"-Feldes eines SSE-Ereignisses.
     */
    private void processDataPayload(String dataPayload) {
        try {
            // Zuerst versuchen, als MastodonStatus zu parsen (für 'update' oder 'status.update' Events)
            MastodonDtos.MastodonStatus status = objectMapper.readValue(dataPayload, MastodonDtos.MastodonStatus.class);
            List<String> contents = new ArrayList<>();

            final String content = Jsoup.parse(status.content()).text();
//            final String content = status.content().replaceAll("<[^>]*>", "").trim();
            contents.add(content);

            final List<String> urls = extractLinksFromHtml(status.content());
            if(!urls.isEmpty()){
                for (String url : urls) {
                    final String article = JsoupParser.getArticle(url);
                    if(article != null){
                        final List<String> splitText = StarredMastodonPosts.splitByLength(article, 500);
                        contents.addAll(splitText);
                    }
                }


            }

                LOG.info("Start OLLAMA Request");
                EmbeddingRequest request = new EmbeddingRequest("granite-embedding:278m", contents, true);

                ollamaRestClient.generateEmbeddingsTest(request).subscribe().with(s -> {

                    final List<double[]> collect = s.embeddings().stream().map(doubles -> doubles.stream().mapToDouble(Double::doubleValue).toArray()).toList();
//                    s.add(response.embeddings().getFirst().stream().mapToDouble(Double::doubleValue).toArray());

                    LOG.info("Vektoren erhalten: " + collect.size());

                    final double[] profileVector = StarredMastodonPosts.createProfileVector(collect);

                    LOG.infof("Empfangener Status (ID: %s, Account: %s, Inhalt: \"%s\" Vektorlänge: %s)\n",
                            status.id(), status.account().username(), content, profileVector.length);

                    if(status.card() != null){
                        LOG.info("Hat Card: " + status.card().url());
                    }
//                    LOG.infof("Embedding: %s", s.embeddings().getFirst());
                });




        } catch (JsonProcessingException e) {
            // Wenn es kein MastodonStatus-JSON ist, könnte es ein Delete-Ereignis sein (nur ein ID-String)
            try {
                // Versuchen, die Payload als Long (ID) zu parsen
                Long.parseLong(dataPayload);
                LOG.infof("Empfangenes Delete-Ereignis (ID: %s)\n", dataPayload);
            } catch (NumberFormatException nfe) {
                // Es ist weder ein Status-JSON noch eine numerische ID.
                // Behandeln Sie andere unbekannte Datentypen oder protokollieren Sie den Fehler.
                Log.error("Unbekanntes Datenformat empfangen: " + dataPayload + " - Fehler: " + nfe.getMessage(), nfe);
            }
        } catch (Exception e) {
            // Allgemeine Fehlerbehandlung für andere unerwartete Probleme
            Log.error("Fehler beim Verarbeiten des Ereignisses: " + dataPayload + " - Fehler: " + e.getMessage(), e);
        }
    }


    public static List<String> extractLinksFromHtml(String htmlContent) {
        List<String> links = new ArrayList<>();
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            System.err.println("Fehler: HTML-Inhalt ist null oder leer.");
            return links;
        }

        try {
            // Parsen des HTML-Inhalts mit Jsoup
            Document doc = Jsoup.parse(htmlContent);

            // Alle 'a'-Tags (Anker-Tags) auswählen
            Elements linkElements = doc.select("a[href]");

            // Iterieren über die gefundenen Elemente und Extrahieren des 'href'-Attributs
            for (Element linkElement : linkElements) {
                String link = linkElement.attr("href");
                if (!link.isEmpty()) { // Sicherstellen, dass der Link nicht leer ist
                    links.add(link);
                }
            }
        } catch (Exception e) {
            // Fehlerbehandlung: Wenn beim Parsen ein Problem auftritt
            LOG.error("Fehler beim Extrahieren der Links: " + e.getMessage(), e);
        }
        return links;
    }
}
