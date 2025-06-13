package com.hexix;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URL;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class FeedReaderService {

    public List<SyndEntry> readFeedEntries(String feedUrl) {
        try (XmlReader reader = new XmlReader(new URL(feedUrl))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            return feed.getEntries();
        } catch (Exception e) {
            System.err.println("Fehler beim Lesen des Feeds: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
