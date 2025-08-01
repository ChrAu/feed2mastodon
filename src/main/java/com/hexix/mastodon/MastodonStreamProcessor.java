package com.hexix.mastodon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexix.JsoupParser;
import com.hexix.ai.OllamaRestClient;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.MastodonClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure; // Wichtig für runOn()
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;


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
    MastodonClient mastodonClient;

    @Inject
    @RestClient
    OllamaRestClient ollamaRestClient;

    // Injiziert den Jackson ObjectMapper für die JSON-Deserialisierung
    @Inject
    ObjectMapper objectMapper;


    @ConfigProperty(name = "mastodon.access.token")
    String accessToken;

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
        final Multi<String> directStream = mastodonStreamingService.mastodonStreamingDirect("Bearer " + accessToken);
        directStream.onFailure(throwable -> false).retry().withBackOff(Duration.ofSeconds(5), Duration.ofSeconds(10)).atMost(Long.MAX_VALUE)
                .onItem().call(this::processDirectPayload)
                .subscribe().with(dataPayload -> Log.debugf("Direct-Mastodon-Payload erfolgreich verarbeitet: %s", dataPayload), failure -> Log.error("Fehler im Mastodon Stream (Direct) nach Wiederholungsversuchen: " + failure.getMessage(), failure));



        final Multi<String> stringMulti = mastodonStreamingService.streamPublicTimeline("Bearer " + privateAccessToken);
        stringMulti
                // Wenn ein Fehler auftritt (z.B. Verbindungsabbruch), versuche die Verbindung erneut herzustellen.
                // retry().withBackoff(initialBackoff, maxBackoff): Versucht es erneut mit exponentiellem Backoff.
                // atMost(Long.MAX_VALUE): Versucht es unendlich oft.
                .onFailure(throwable -> {
                    Log.error("Fehler beim Stream-Empfang: " + throwable.getMessage(), throwable);
                    return false; // Gibt an, dass die Wiederholung fortgesetzt werden soll
                })
                .retry().withBackOff(Duration.ofSeconds(5), Duration.ofSeconds(10)).atMost(Long.MAX_VALUE)
                // WICHTIGE ÄNDERUNG: onItem().call() stellt sicher, dass jede processDataPayload-Ausführung
                // abgeschlossen ist, bevor das nächste Element aus dem Stream verarbeitet wird.
                .onItem().call(this::processDataPayload)
                .subscribe().with(
                        // Dieser Consumer wird aufgerufen, nachdem jede processDataPayload-Uni abgeschlossen ist.
                        // Der 'dataPayload' hier ist das ursprüngliche String-Payload vom Multi.
                        dataPayload -> {
                            Log.debugf("Mastodon-Payload erfolgreich verarbeitet: %s", dataPayload);
                        },
                        failure -> {
                            // Fehler beim Stream-Empfang (nach allen Wiederholungsversuchen)
                            Log.error("Fehler im Mastodon Stream nach Wiederholungsversuchen: " + failure.getMessage(), failure);
                            // Hier könnten Sie weitere Aktionen auslösen, z.B. Benachrichtigungen.
                        }
                );
    }

    private Uni<?> processDirectPayload(String dataPayload) {
        try {
            MastodonDtos.DirectStatus directStatus = objectMapper.readValue(dataPayload, MastodonDtos.DirectStatus.class);
            final String replyId = directStatus.lastStatus().inReplyToId();

            final String rawContent = directStatus.lastStatus().content();
            final String content = Jsoup.parse(rawContent).text();
            boolean noUrl;
            if(content.contains("negativ")) {

                if (content.toLowerCase().contains("no_url")) {
                    noUrl = true;
                } else {
                    noUrl = false;
                }

                final String[] negativs = content.split("negativ");

                Double negativeWeight = Double.parseDouble(negativs[negativs.length - 1].trim());


                return Uni.createFrom().item(() -> {


                            try {
                                createPostAndUpdate(replyId, negativeWeight, noUrl);
                                mastodonClient.deleteStatus("Bearer " + privateAccessToken, directStatus.lastStatus().id());
                            }catch (Exception e){
                                LOG.errorf(e, "Fehler beim löschen der Direct Nachricht Id: %s", directStatus.lastStatus().id());
                            }
                            // Diese blockierende Operation wird nun auf einem Worker-Thread ausgeführt
                            return Uni.createFrom().voidItem(); // Uni<Void> benötigt einen Wert, null ist für Void ok
                        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .onFailure().invoke(e -> {
                            Log.errorf(e, "Fehler beim Speichern des Mastodon-Posts (ID: %s): %s", replyId, e.getMessage());
                        })
                        .onItem().ignore().andContinueWithNull();

            }

            } catch (JsonProcessingException e) {
            LOG.warn("Es ist ein Fehler beim Parsen des Mastodon Status aufgetreten", e);
        }


        return Uni.createFrom().voidItem();
    }


    @Transactional
    void createPostAndUpdate(final String replyId, final Double negativeWeight, boolean noUrl) {
        PublicMastodonPostEntity post = PublicMastodonPostEntity.findByMastodonId(replyId);

        if(post == null){
            MastodonDtos.MastodonStatus status = mastodonClient.getStatus(replyId, "Bearer " + privateAccessToken);
            post = getPublicMastodonPostEntity(status, noUrl);
            savePostInPipeline(post);
        }else{
            if(noUrl){
                post.setUrlText(null);
                post.removeEmbeddingVektor();
            }
        }

        try{
            mastodonClient.unBoostStatus(replyId, "Bearer " + accessToken);
        }catch (Exception e){
            LOG.errorf(e,"Fehler unboost status Id: %s", replyId);
        }

        post.setNoURL(noUrl);


        post.setNegativeWeight(negativeWeight);

        // Zuerst versuchen, als MastodonStatus zu parsen (für 'update' oder 'status.update' Events)

    }


    /**
     * Verarbeitet die empfangene Daten-Payload vom Mastodon-Stream.
     * Versucht, die Payload als MastodonStatus zu parsen oder als Delete-Ereignis zu identifizieren.
     * Diese Methode gibt nun ein Uni<Void> zurück, das abgeschlossen wird, wenn die Verarbeitung
     * (einschließlich des asynchronen Ollama-Aufrufs) abgeschlossen ist.
     *
     * @param dataPayload Der rohe String-Inhalt des "data"-Feldes eines SSE-Ereignisses.
     * @return Ein Uni<Void>, das den Abschluss der Verarbeitung anzeigt.
     */
    private Uni<Void> processDataPayload(String dataPayload) {
        try {



            MastodonDtos.MastodonStatus status = objectMapper.readValue(dataPayload, MastodonDtos.MastodonStatus.class);

            if(status.account().acct().contains("heiseonlineenglish@social.heise.de")){
                return Uni.createFrom().voidItem();
            }

            final PublicMastodonPostEntity post = getPublicMastodonPostEntity(status, false);

            // WICHTIGE ÄNDERUNG: post.persist() auf einen Worker-Thread auslagern
            return Uni.createFrom().item(() -> {
                        final PublicMastodonPostEntity currentEntity = PublicMastodonPostEntity.findByMastodonId(status.id());

                        if (currentEntity == null) {
                            // Zuerst versuchen, als MastodonStatus zu parsen (für 'update' oder 'status.update' Events)
                            savePostInPipeline(post);
                        }


                         // Diese blockierende Operation wird nun auf einem Worker-Thread ausgeführt
                        return Uni.createFrom().voidItem(); // Uni<Void> benötigt einen Wert, null ist für Void ok
                    }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .onFailure().invoke(e -> {
                        Log.errorf(e,"Fehler beim Speichern des Mastodon-Posts (ID: %s): %s", status.id(), e.getMessage());
                    })
                    .onItem().ignore().andContinueWithNull();


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
            return Uni.createFrom().voidItem(); // Sofortiger Abschluss für nicht-Status-Payloads
        } catch (Exception e) {
            // Allgemeine Fehlerbehandlung für andere unerwartete Probleme
            Log.error("Fehler beim Verarbeiten des Ereignisses: " + dataPayload + " - Fehler: " + e.getMessage(), e);
            return Uni.createFrom().voidItem(); // Sofortiger Abschluss bei allgemeinen Fehlern
        }
    }




    private static PublicMastodonPostEntity getPublicMastodonPostEntity(final MastodonDtos.MastodonStatus status, boolean noUrl) {
        final PublicMastodonPostEntity post = new PublicMastodonPostEntity();
        post.setMastodonId(status.id());
        post.setStatusOriginalUrl(status.url());

        final String text = Jsoup.parse(status.content()).text();

        post.setNoURL(noUrl);

        LOG.infof("Empfangener Status (ID: %s, Account: %s, Inhalt: \"%s\", URL: %s)\n",
                status.id(), status.account().username(), text.substring(0, Math.min(20, text.length())) + "...", post.getStatusOriginalUrl());


        return post;
    }

    @Transactional
    public void savePostInPipeline(final PublicMastodonPostEntity post) {
        post.persist();
    }



}
