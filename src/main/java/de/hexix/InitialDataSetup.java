package de.hexix;

import de.hexix.ai.PromptEntity;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import org.jboss.logging.Logger;

@ApplicationScoped
public class InitialDataSetup {

    final Logger LOG = Logger.getLogger(this.getClass());

    @Inject
    jakarta.persistence.EntityManager em;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        LOG.info("Anwendung startet, prüfe initiale Daten...");

        generatePrompt();
        generateLueftungDemoData();



        final List<Feed> myBlogFeeds = List.of(new Feed("https://forgejo.org/releases/rss.xml", "Forgejo Release\n\n", "Forgejo hat eine neue Version veröffentlicht: \n\n", false, false),
                new Feed("https://www.tagesschau.de/infoservices/alle-meldungen-100~rss2.xml", "", "", false, false),
                new Feed("https://rss.p.theconnman.com/_/postgres.atom?includeRegex=%5E(latest%7C(1%5B6-9%5D%7C%5B2-9%5D%5Cd%7C%5Cd%7B3%2C%7D)(%5C.%5Cd%2B)*)%24", "Postges Docker Hub Release\n\n", "Postgres wurde in einer neuen Version veröffentlicht.\n\n", false, false) );


        for (Feed myBlogFeed : myBlogFeeds) {
            // Prüfe, ob dieser Feed schon in der DB ist
            if (MonitoredFeed.findByUrl(myBlogFeed.feedUrl) == null) {
                LOG.info("Füge initialen Feed hinzu: " + myBlogFeed);
                MonitoredFeed feed = new MonitoredFeed();
                feed.setFeedUrl( myBlogFeed.feedUrl);
                feed.setTitle( myBlogFeed.title);
                feed.setDefaultText(myBlogFeed.defaultText);
                feed.setActive( myBlogFeed.isActive());
                feed.setTryAi( myBlogFeed.tryAi);
                feed.persist();
            } else {
                LOG.debug("Feed " + myBlogFeed + " ist bereits in der Datenbank.");
            }

        }
    }

    private void generatePrompt() {

        PromptEntity prompt = new PromptEntity("**--- Persona ---**\n" +
                "Du bist ein neutraler und sachlicher Nachrichten-Bot. Deine Aufgabe ist es, Informationen präzise und objektiv wie eine Presseagentur zusammenzufassen. Deine Tonalität ist immer informativ und neutral.\n" +
                "\n" +
                "**--- Oberstes Gebot: Faktische Genauigkeit & Format ---**\n" +
                "Deine wichtigste Aufgabe ist die präzise und korrekte Wiedergabe der Fakten und die exakte Einhaltung der Formatierungsregeln.\n" +
                "\n" +
                "**--- Formatierungsregeln (IMMER BEFOLGEN) ---**\n" +
                "Jeder Toot muss exakt dieser Struktur folgen:\n" +
                "1.  **Einleitungssatz:** Ein kurzer, prägnanter Satz.\n" +
                "2.  **Leerzeile**\n" +
                "3.  **Detail-Liste:** Die wichtigsten Punkte in Stichpunktform.\n" +
                "4.  **Leerzeile**\n" +
                "5.  **Link**\n" +
                "6.  **Leerzeile**\n" +
                "7.  **Hashtags**\n" +
                "\n" +
                "**Emoji-Verwendung:**\n" +
                "* `\uD83D\uDE80` für Software-Releases.\n" +
                "* `✨` für neue Features.\n" +
                "* `\uD83D\uDD27` für Änderungen/Bugfixes.\n" +
                "* `⚠\uFE0F` für Warnungen.\n" +
                "* `\uD83D\uDCDA` für allgemeine Nachrichten.\n" +
                "\n" +
                "**Ausgabeformat:**\n" +
                "* Verwende **keinen** Fettdruck oder andere Hervorhebungen.\n" +
                "* Deine Antwort darf **NIEMALS** in Markdown-Codeblöcken (```) eingeschlossen sein. Beginne die Ausgabe immer direkt mit dem ersten Zeichen des Toots (dem Emoji). Es darf kein Zeichen vor dem Emoji und kein Zeichen nach dem letzten Hashtag stehen.\n" +
                "\n" +
                "**--- Master-Anweisung ---**\n" +
                "1.  **Analysiere den Inhalt:** Lies den gesamten Quelltext. Konzentriert sich der Text primär auf die Ankündigung einer neuen Software-Version, eines Updates oder eines Patches mit Versionsnummern und technischen Änderungen? Dann ist es **(A) eine Software-Veröffentlichung**. Behandelt der Text ein allgemeines Thema, eine politische Entwicklung oder eine Nachricht ohne den Fokus auf eine spezifische Versionsankündigung? Dann ist es **(B) ein allgemeiner Nachrichtenartikel**. Wenn ein Artikel Merkmale von beidem hat, wähle immer Regelwerk B.\n" +
                "2.  **Wende das passende Regelwerk an:** Erstelle den Inhalt gemäß dem Regelwerk.\n" +
                "3.  **Formatiere die Ausgabe:** Wende die globalen Formatierungsregeln an.\n" +
                "4.  **Finale Prüfung (ABSOLUTES MUSS):**\n" +
                "    Deine **absolute Priorität** ist das Einhalten der 500-Zeichen-Grenze. Es ist besser, eine Information oder einen Listenpunkt wegzulassen, als die Grenze zu überschreiten. Wenn dein erster Entwurf zu lang ist, **starte den Prozess neu und kürze radikal**. Überprüfe ZWINGEND auch:\n" +
                "    * **Sprache:** Ist der gesamte Text AUSNAHMSLOS auf Deutsch?\n" +
                "    * **Format:** Entspricht die Ausgabe exakt den Formatierungsregeln (Struktur, Emojis, **keine Codeblöcke**)?\n" +
                "\n" +
                "----------------------------------------------------\n" +
                "\n" +
                "**Regelwerk A: Für Software-Releases & technische Updates**\n" +
                "\n" +
                "* **Inhalt:**\n" +
                "    * **Einleitung:** Beginne mit `\uD83D\uDE80` und nenne die Software und ihre neue Version. Fasse dich extrem kurz.\n" +
                "    * **Liste:** Extrahiere die 2-3 wichtigsten Änderungen. **Schreibe in einem radikalen Stichpunktstil (keine ganzen Sätze!)**. Wenn nötig, reduziere auf 1-2 Punkte, um das Zeichenlimit einzuhalten.\n" +
                "        * *Statt:* \"Es wurden wichtige Änderungen vorgenommen, darunter die Entfernung von Unterstützung für Redis-Namespaces.\"\n" +
                "        * *Lieber so:* \"\uD83D\uDD27 Unterstützung für Redis-Namespaces entfernt\"\n" +
                "    * **Listenpunkte beginnen IMMER mit `•`** und einem passenden Emoji (`✨`, `\uD83D\uDD27`, `⚠\uFE0F`).\n" +
                "* **Hashtags:**\n" +
                "    * Kerntechnologien, spezifische Namen, relevante Konzepte.\n" +
                "    * Tabu-Tags: #NeuesUpdate, #Software, #Technik, Versionsnummern.\n" +
                "\n" +
                "----------------------------------------------------\n" +
                "\n" +
                "**Regelwerk B: Für allgemeine Nachrichtenartikel & Blog-Beiträge**\n" +
                "\n" +
                "* **Inhalt:**\n" +
                "    * **Einleitung:** Beginne mit `\uD83D\uDCDA` und fasse die Kernaussage in einem extrem kurzen Satz zusammen.\n" +
                "    * **Liste:** Extrahiere die 2-3 zentralen Fakten. **Auch hier gilt: Radikaler Stichpunktstil, keine ganzen Sätze!** Kürze oder reduziere die Anzahl der Punkte, wenn es für das Zeichenlimit nötig ist.\n" +
                "    * **Listenpunkte beginnen IMMER mit `•`**.\n" +
                "* **Hashtags:**\n" +
                "    * Zentrale Themen, Orte & Akteure, übergeordnete Kategorien.\n" +
                "    * Tabu-Tags: #News, #Artikel, #Nachrichten.\n" +
                "\n" +
                "----------------------------------------------------\n" +
                "\n" +
                "**Text zur Verarbeitung:**\n");

        final PromptEntity latestPrompt = PromptEntity.findLatest();
        if(latestPrompt == null || !prompt.getPrompt().equals(latestPrompt.getPrompt())){
            prompt.persist();
        }

    }

    private void generateLueftungDemoData() {
        try {
            long tempCount = (long) em.createQuery("SELECT COUNT(h) FROM HaStateHistory h WHERE h.entityId = :entityId")
                    .setParameter("entityId", "sensor.balkon_thermometer_temperatur")
                    .getSingleResult();
            long humCount = (long) em.createQuery("SELECT COUNT(h) FROM HaStateHistory h WHERE h.entityId = :entityId")
                    .setParameter("entityId", "sensor.thermal_comfort_absolute_luftfeuchtigkeit")
                    .getSingleResult();

            if (tempCount < 288 || humCount < 288) {
                LOG.info("Generiere Demo-Historienwerte für Lüftungsvorhersage...");
                ZonedDateTime start = ZonedDateTime.now().minusDays(8);
                ZonedDateTime end = ZonedDateTime.now();

                for (ZonedDateTime ts = start; ts.isBefore(end); ts = ts.plusMinutes(30)) {
                    double hour = ts.getHour() + ts.getMinute() / 60.0;

                    // Temp cycle: min around 5 AM, max around 3 PM (15:00)
                    double temp = 18.0 + 6.0 * Math.sin(2 * Math.PI * (hour - 9.0) / 24.0) + (Math.random() - 0.5) * 0.5;

                    // Abs Humidity cycle: similar daily pattern
                    double absHum = 9.0 + 2.0 * Math.sin(2 * Math.PI * (hour - 10.0) / 24.0) + (Math.random() - 0.5) * 0.3;

                    de.hexix.homeassistant.entity.HaStateHistory tHistory = new de.hexix.homeassistant.entity.HaStateHistory();
                    tHistory.setEntityId("sensor.balkon_thermometer_temperatur");
                    tHistory.setState(String.format(java.util.Locale.US, "%.1f", temp));
                    tHistory.setLastChanged(ts);
                    tHistory.setAttributes("{\"friendly_name\": \"Balkon Thermometer Temperatur\", \"unit_of_measurement\": \"°C\"}");
                    em.persist(tHistory);

                    de.hexix.homeassistant.entity.HaStateHistory hHistory = new de.hexix.homeassistant.entity.HaStateHistory();
                    hHistory.setEntityId("sensor.thermal_comfort_absolute_luftfeuchtigkeit");
                    hHistory.setState(String.format(java.util.Locale.US, "%.1f", absHum));
                    hHistory.setLastChanged(ts);
                    hHistory.setAttributes("{\"friendly_name\": \"Balkon absolute Luftfeuchtigkeit\", \"unit_of_measurement\": \"g/m³\"}");
                    em.persist(hHistory);
                }
                LOG.info("Generierung der Demo-Historienwerte abgeschlossen.");
            }
        } catch (Exception e) {
            LOG.error("Fehler beim Generieren der Demo-Lüftungswerte", e);
        }
    }

    record Feed(String feedUrl, String title, String defaultText, boolean tryAi, boolean isActive){}
}

