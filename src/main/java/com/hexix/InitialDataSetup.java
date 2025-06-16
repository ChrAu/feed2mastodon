package com.hexix;

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

        final List<Feed> myBlogFeeds = List.of(new Feed("https://forgejo.org/releases/rss.xml", "Forgejo Release\n\n", "Forgejo hat eine neue Version veröffentlicht: \n\n", true),
                new Feed("https://www.tagesschau.de/infoservices/alle-meldungen-100~rss2.xml", "", "", false),
                new Feed("https://rss.p.theconnman.com/_/postgres.atom?includeRegex=%5E(latest%7C(1%5B6-9%5D%7C%5B2-9%5D%5Cd%7C%5Cd%7B3%2C%7D)(%5C.%5Cd%2B)*)%24", "Postges Docker Hub Release\n\n", "Postgres wurde in einer neuen Version veröffentlicht.\n\n", true) );


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

    record Feed(String feedUrl, String title, String defaultText, boolean tryAi){}
}
