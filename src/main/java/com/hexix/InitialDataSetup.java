package com.hexix;

import com.hexix.ai.PromptEntity;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.util.List;

import org.jboss.logging.Logger;

@ApplicationScoped
public class InitialDataSetup {

    final Logger LOG = Logger.getLogger(this.getClass());

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        LOG.info("Anwendung startet, prüfe initiale Daten...");

        generatePrompt();



        final List<Feed> myBlogFeeds = List.of(new Feed("https://forgejo.org/releases/rss.xml", "Forgejo Release\n\n", "Forgejo hat eine neue Version veröffentlicht: \n\n", false),
                new Feed("https://www.tagesschau.de/infoservices/alle-meldungen-100~rss2.xml", "", "", false),
                new Feed("https://rss.p.theconnman.com/_/postgres.atom?includeRegex=%5E(latest%7C(1%5B6-9%5D%7C%5B2-9%5D%5Cd%7C%5Cd%7B3%2C%7D)(%5C.%5Cd%2B)*)%24", "Postges Docker Hub Release\n\n", "Postgres wurde in einer neuen Version veröffentlicht.\n\n", false) );


        for (Feed myBlogFeed : myBlogFeeds) {
            // Prüfe, ob dieser Feed schon in der DB ist
            if (MonitoredFeed.findByUrl(myBlogFeed.feedUrl) == null) {
                LOG.info("Füge initialen Feed hinzu: " + myBlogFeed);
                MonitoredFeed feed = new MonitoredFeed();
                feed.feedUrl = myBlogFeed.feedUrl;
                feed.title = myBlogFeed.title;
                feed.defaultText = myBlogFeed.defaultText;
                feed.isActive = true;
                feed.tryAi = myBlogFeed.tryAi;
                feed.persist();
            } else {
                LOG.info("Feed " + myBlogFeed + " ist bereits in der Datenbank.");
            }

        }
    }

    private void generatePrompt() {

        PromptEntity prompt = new PromptEntity("**--- Oberstes Gebot: Faktische Genauigkeit ---**\n" +
                "Deine wichtigste Aufgabe ist die präzise und korrekte Wiedergabe der Fakten aus dem Quelltext. Formuliere Sätze so, dass der ursprüngliche Sinn und die Zusammenhänge (z.B. wer handelt wo? wer ist wovon betroffen?) exakt erhalten bleiben. **Vereinfache, aber verfälsche niemals den Inhalt.** Missverständliche oder mehrdeutige Formulierungen sind zu vermeiden. **Ein Toot darf die Zeichenlänge von 500 Zeichen nicht überschreiten!\n" +
                "\n" +
                "**--- Master-Anweisung ---**\n" +
                "1.  **Analysiere den Inhalt:** Lies den Text unter Beachtung des obersten Gebots und entscheide, ob es sich um (A) eine Software-Veröffentlichung oder (B) einen allgemeinen Nachrichtenartikel handelt.\n" +
                "2.  **Wende das passende Regelwerk an:** Folge den spezifischen Anweisungen für den identifizierten Inhaltstyp. (Schreibe keine Metadaten (z.B. (A) oder **Regelwerk A) mit in die Antwort)\n" +
                "3.  **Link:** Füge den Originallink IMMER dem Posts als reinen Text (inkl. https://) hinzu, dies ist sehr wichtig!!!\n" +
                "4. **Hashtags:** Hashtags stehen IMMER am Ende des Posts. Dies ist wichtig, da Mastodon so die Hashtags als solche besser verarbeiten kann.**\n" +
                "\n" +
                "----------------------------------------------------\n" +
                "\n" +
                "**Regelwerk A: Für Software-Releases & technische Updates**\n" +
                "\n" +
                "* **Stil des Posts:** Schreibe kurz, prägnant und informativ für ein Fachpublikum. Konzentriere dich auf die Kernneuigkeit.\n" +
                "* **Anleitung für Hashtags:**\n" +
                "    * **Kerntechnologien:** #Python, #Docker, #Postgres etc.\n" +
                "    * **Spezifische Versionen:** #Postgres17, #Python312.\n" +
                "    * **Relevante Konzepte:** #Containerisierung, #Datenbank, #DevOps.\n" +
                "    * **Tabu-Tags:** Vermeide generische Füllwörter wie #NeuesUpdate, #Software, #Technik.\n" +
                "\n" +
                "----------------------------------------------------\n" +
                "\n" +
                "**Regelwerk B: Für allgemeine Nachrichtenartikel & Blog-Beiträge**\n" +
                "\n" +
                "* **Stil des Posts:** Fasse die Kernaussage und die wichtigsten Fakten zusammen. **Achte strikt darauf, die Beziehungen zwischen Akteuren und Orten korrekt wiederzugeben.** Du kannst eine offene Frage stellen, um zur Diskussion anzuregen, solange diese die Fakten nicht verzerrt.\n" +
                "* **Anleitung für Hashtags:**\n" +
                "    * **Zentrale Themen:** #NahostKonflikt, #Klimawandel, #Netzpolitik.\n" +
                "    * **Orte & Akteure:** #Israel, #Iran, #Tschechien etc.\n" +
                "    * **Übergeordnete Kategorien:** #Politik, #Gesellschaft, #Diplomatie.\n" +
                "    * **Tabu-Tags:** Vermeide Tags wie #News, #Artikel, #Nachrichten, #Interessant.\n" +
                "\n" +
                "----------------------------------------------------\n" +
                "\n" +
                "**Text zur Verarbeitung:**");

        final PromptEntity latestPrompt = PromptEntity.findLatest();
        if(latestPrompt == null || !prompt.prompt.equals(latestPrompt.prompt)){
            prompt.persist();
        }

    }

    record Feed(String feedUrl, String title, String defaultText, boolean tryAi){}
}
