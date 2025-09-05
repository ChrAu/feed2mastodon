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

    @Inject
    PublicMastodonPostRepository publicMastodonPostRepository;


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
        subscribeToMastodonPublicStream();
        subscribeToMastodonDirectStream();
    }

    /**
     * Abonniert den Mastodon-Stream und definiert die Verarbeitungs- und Wiederverbindungslogik.
     */
    private void subscribeToMastodonPublicStream() {
        final Multi<String> publicStream = mastodonStreamingService.streamPublicTimeline("Bearer " + privateAccessToken);
        publicStream
                // Dieselbe robuste Logik für den öffentlichen Stream.
                .onCompletion().failWith(new RuntimeException("Public-Stream wurde unerwartet beendet. Starte Wiederverbindung."))
                // KORREKTUR: Protokolliert den Fehler und gibt `true` zurück, um den Wiederholungsversuch zu signalisieren.
                .onFailure().invoke(throwable -> LOG.error("Fehler im Public-Stream. Wiederholungsversuch wird gestartet...", throwable)).onFailure()
                .retry().withBackOff(Duration.ofSeconds(5), Duration.ofMinutes(1)).withJitter(0.5).indefinitely()
                .onItem().call(this::processDataPayload)
                .subscribe().with(
                        dataPayload -> LOG.debugf("Public-Payload erfolgreich verarbeitet."),
                        failure -> {
                            LOG.fatal("Public-Stream ist endgültig fehlgeschlagen. Starte den gesamten Abonnementprozess in 30 Sekunden neu.", failure);
                            Uni.createFrom().item(1)
                                    .onItem().delayIt().by(Duration.ofSeconds(30))
                                    .invoke(this::subscribeToMastodonPublicStream)
                                    .subscribe().with(item -> LOG.info("Neustart des Public-Stream-Abonnements eingeleitet."));
                        }
                );
    }

    /**
     * Abonniert den Mastodon-Stream und definiert die Verarbeitungs- und Wiederverbindungslogik.
     */
    private void subscribeToMastodonDirectStream() {
        final Multi<String> directStream = mastodonStreamingService.mastodonStreamingDirect("Bearer " + accessToken);
        directStream
                // WICHTIGE ÄNDERUNG: Behandelt eine unerwartete Beendigung des Streams als Fehler.
                // Ein Event-Stream sollte theoretisch nie von selbst enden. Wenn doch, wollen wir uns neu verbinden.
                .onCompletion().failWith(new RuntimeException("Direct-Stream wurde unerwartet beendet. Starte Wiederverbindung."))
                // Protokolliert den Fehler (entweder von der Verbindung oder von onCompletion) und löst den Wiederholungsversuch aus.
                .onFailure().invoke(throwable -> LOG.error("Fehler im Direct-Stream. Wiederholungsversuch wird gestartet...", throwable)).onFailure()
                // Verbesserte Wiederholungslogik mit exponentiellem Backoff und Jitter.
                // Startet bei 5s, geht bis zu 1 Minute hoch und versucht es unendlich oft.
                .retry().withBackOff(Duration.ofSeconds(5), Duration.ofMinutes(1)).withJitter(0.5).indefinitely()
                // Verarbeitet jedes Element asynchron. `call` sorgt für Back-Pressure, d.h. das nächste Element
                // wird erst angefordert, wenn die Verarbeitung des aktuellen abgeschlossen ist.
                .onItem().call(this::processDirectPayload)
                // Abonniert den Stream und startet die Verarbeitung.
                .subscribe().with(
                        dataPayload -> LOG.debugf("Direct-Payload erfolgreich verarbeitet."),
                        // Dieser Block sollte dank .indefinitely() nie erreicht werden, ist aber ein Sicherheitsnetz.
                        failure -> {
                            LOG.fatal("Direct-Stream ist endgültig fehlgeschlagen. Starte den gesamten Abonnementprozess in 30 Sekunden neu.", failure);
                            Uni.createFrom().item(1)
                                    .onItem().delayIt().by(Duration.ofSeconds(30))
                                    .invoke(this::subscribeToMastodonDirectStream)
                                    .subscribe().with(item -> LOG.info("Neustart des Direct-Stream-Abonnements eingeleitet."));
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

                noUrl = content.toLowerCase().contains("no_url");

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
        PublicMastodonPostEntity post = publicMastodonPostRepository.findByMastodonId(replyId).orElse(null);

        if(post == null){
            MastodonDtos.MastodonStatus status = mastodonClient.getStatus(replyId, "Bearer " + privateAccessToken);
            post = getPublicMastodonPostEntity(status, noUrl);
            publicMastodonPostRepository.persist(post);
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
                        final PublicMastodonPostEntity currentEntity = publicMastodonPostRepository.findByMastodonId(status.id()).orElse(null);

                        if (currentEntity == null) {
                            // Zuerst versuchen, als MastodonStatus zu parsen (für 'update' oder 'status.update' Events)
                            publicMastodonPostRepository.persist(post);
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




    private PublicMastodonPostEntity getPublicMastodonPostEntity(final MastodonDtos.MastodonStatus status, boolean noUrl) {
        final PublicMastodonPostEntity post = new PublicMastodonPostEntity();
        post.setMastodonId(status.id());
        post.setStatusOriginalUrl(status.url());

        final String text = Jsoup.parse(status.content()).text();

        final TextEntity textEntity = new TextEntity(text);
        if(textEntity.getText() != null && !textEntity.getText().isBlank()){
            post.setPostText(textEntity);
        }



        post.setNoURL(noUrl);

        LOG.infof("Empfangener Status (ID: %s, Account: %s, Inhalt: \"%s\", URL: %s)\n",
                status.id(), status.account().username(), text.substring(0, Math.min(20, text.length())) + "...", post.getStatusOriginalUrl());


        return post;
    }





}
