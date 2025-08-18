package com.hexix.mastodon.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MastodonDtos {

    static Logger LOG = Logger.getLogger(MastodonDtos.class.getName());
    // Record für das Senden eines neuen Status
    public record StatusPayload(String status, String visibility, String language) {}

    // Record, um die Antwort von /verify_credentials abzubilden
    // Wir brauchen hier nur die ID.
    public record MastodonAccount(String id, String username, String acct) {}

    /**
     * Request-Body für das Boosten eines Status, um die Sichtbarkeit zu steuern.
     */
    public record BoostStatusRequest(MastodonStatus.StatusVisibility visibility) {}

    public record DirectStatus(String id, boolean unread, List<MastodonAccount> accounts, @JsonProperty("last_status") MastodonStatus lastStatus){}

    public record MastodonSearchResult(
            List<MastodonAccount> accounts,
            List<MastodonStatus> statuses
    ) {}

    // Record, um einen einzelnen empfangenen Status abzubilden
    // @JsonProperty wird verwendet, um JSON-Felder (snake_case) auf Java-Felder (camelCase) zu mappen.
    public record MastodonStatus(
            String id,
            String url,
            @JsonProperty("created_at") ZonedDateTime createdAt,// Jackson wandelt den String automatisch in ein Datum um
            MastodonAccount account,
            String content,
            StatusVisibility visibility,
            boolean sensitive,
            @JsonProperty("spoiler_text") String spoilerText,
            @JsonProperty("media_attachments") List<MediaAttachment> mediaAttachments,
            Application application,
            @JsonProperty("reblogs_count") Integer reblogsCount,
            @JsonProperty("favourites_count") Integer favouritesCount,
            @JsonProperty("replies_count") Integer repliesCount,
            Boolean reblogged,
            @JsonProperty("reblog") Optional<MastodonStatus> reblog,
            String language,
            String text,
            PreviewCard card,
            @JsonProperty("in_reply_to_id")
            String inReplyToId

) {
        public enum StatusVisibility{
            PUBLIC("public"),
            UNLISTED("unlisted"),
            PRIVATE("private"),
            DIRECT("direct");

            private final String value;

            StatusVisibility(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }
        }

        public record Application(
                String name,
                String website
        ){}

        public static List<String> extractLinksFromHtml(String htmlContent) {
            List<String> links = new ArrayList<>();
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
//                System.err.println("Fehler: HTML-Inhalt ist null oder leer.");
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


    /**
     * Stellt eine Vorschaukarte dar, die ein Link, ein Foto oder ein Video sein kann.
     *
     * @param url Die URL des Inhalts.
     * @param title Der Titel des Inhalts.
     * @param description Eine Beschreibung des Inhalts.
     * @param type Der Typ der Karte (z. B. "video", "photo", "link").
     * @param author_name Der Name des Autors.
     * @param author_url Die URL zum Profil des Autors.
     * @param provider_name Der Name des Anbieters (z. B. "YouTube", "Flickr").
     * @param provider_url Die URL des Anbieters.
     * @param html Der HTML-Code zum Einbetten des Inhalts.
     * @param width Die Breite des Inhalts in Pixel.
     * @param height Die Höhe des Inhalts in Pixel.
     * @param image Die URL zu einem Vorschaubild.
     * @param embed_url Die URL zum direkten Einbetten des Inhalts.
     * @param blurhash Ein Blurhash-String für das Bild.
     * @param authors Eine Liste von Autoren (normalerweise für Links).
     */
    public record PreviewCard(
            String url,
            String title,
            String description,
            String type,
            String author_name,
            String author_url,
            String provider_name,
            String provider_url,
            String html,
            int width,
            int height,
            String image,
            String embed_url,
            String blurhash,
            List<PreviewCardAuthor> authors
    ) {

        public record PreviewCardAuthor(String name, String url, Optional<MastodonAccount> account){}
    }




    public record MediaAttachment(
            String id,
            MediaType type, // <-- Geändert von String zu MediaType
            String url,
            @JsonProperty("preview_url") Optional<String> previewUrl,
            @JsonProperty("remote_url") Optional<String> remoteUrl,
            @JsonProperty("text_url") Optional<String> textUrl,
            Optional<Meta> meta,
            Optional<String> description,
            Optional<String> blurhash
    ) {

        /**
         * Definiert die verschiedenen Arten von Medienanhängen.
         * Die Annotation @JsonValue wird von Jackson verwendet, um die
         * kleingeschriebenen String-Werte aus dem JSON auf die Enum-Konstanten abzubilden.
         */
        public enum MediaType {
            IMAGE("image"),
            VIDEO("video"),
            GIFV("gifv"),
            AUDIO("audio"),
            UNKNOWN("unknown"); // Fallback für unbekannte Typen

            private final String value;

            MediaType(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }
        }

        /**
         * Metadaten, die verschiedene Details über das Medium enthalten.
         * Die Felder sind als Optional deklariert, da sie je nach Medientyp variieren.
         *
         * @param length Die Dauer als formatierter String (z.B. "0:01:28.65").
         * @param duration Die Dauer in Sekunden.
         * @param fps Bilder pro Sekunde (für Videos).
         * @param size Die Abmessungen als String (z.B. "1280x720").
         * @param width Die Breite in Pixel.
         * @param height Die Höhe in Pixel.
         * @param aspect Das Seitenverhältnis.
         * @param audioEncode Informationen zur Audiokodierung.
         * @param audioBitrate Die Bitrate des Audios.
         * @param audioChannels Die Audiokanäle (z.B. "stereo").
         * @param original Metadaten der Originaldatei.
         * @param small Metadaten der Vorschauversion.
         * @param focus Der Fokuspunkt für Bildausschnitte.
         */
        public record Meta(
                Optional<String> length,
                Optional<Double> duration,
                Optional<Integer> fps,
                Optional<String> size,
                Optional<Integer> width,
                Optional<Integer> height,
                Optional<Double> aspect,
                @JsonProperty("audio_encode") Optional<String> audioEncode,
                @JsonProperty("audio_bitrate") Optional<String> audioBitrate,
                @JsonProperty("audio_channels") Optional<String> audioChannels,
                Optional<Original> original,
                Optional<Small> small,
                Optional<Focus> focus
        ) {}

        /**
         * Metadaten spezifisch für die Originaldatei.
         *
         * @param width Die Breite in Pixel.
         * @param height Die Höhe in Pixel.
         * @param frameRate Die Bildrate (kann ein String wie "100/3" sein).
         * @param duration Die genaue Dauer in Sekunden.
         * @param bitrate Die Bitrate in bps.
         */
        public record Original(
                Optional<Integer> width,
                Optional<Integer> height,
                @JsonProperty("frame_rate") Optional<String> frameRate,
                Optional<Double> duration,
                Optional<Integer> bitrate
        ) {}

        /**
         * Metadaten spezifisch für die kleine Vorschauversion.
         *
         * @param width Die Breite in Pixel.
         * @param height Die Höhe in Pixel.
         * @param size Die Abmessungen als String (z.B. "400x225").
         * @param aspect Das Seitenverhältnis.
         */
        public record Small(
                int width,
                int height,
                String size,
                double aspect
        ) {}

        /**
         * Der Fokuspunkt des Bildes, verwendet für zugeschnittene Vorschaubilder.
         * Die Werte reichen von -1.0 bis 1.0.
         *
         * @param x Die horizontale Position des Fokuspunktes.
         * @param y Die vertikale Position des Fokuspunktes.
         */
        public record Focus(
                double x,
                double y
        ) {}
    }
}
