package com.hexix; // Passe das Paket an deine Struktur an

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class FeedFetcherTest {

    @Test
    void testFetchAndParseRssFeed() throws Exception {
        // 1. RSS Feed abrufen
        String rssXml = RestAssured.when()
                .get("/test-feeds/rss")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_XML)
                .extract().body().asString();

        assertNotNull(rssXml, "RSS XML sollte nicht null sein.");
        assertTrue(rssXml.contains("<title>Test RSS Feed</title>"), "RSS XML Titel nicht gefunden.");

        // 2. RSS Feed parsen mit Rome
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new StringReader(rssXml)); // XmlReader geht auch für Encoding-Handling

        // 3. Daten prüfen
        assertNotNull(feed, "Geparster RSS Feed sollte nicht null sein.");
        assertEquals("Test RSS Feed", feed.getTitle(), "RSS Feed Titel stimmt nicht überein.");
        assertEquals("This is a test RSS feed.", feed.getDescription(), "RSS Feed Beschreibung stimmt nicht überein.");
        assertEquals("http://localhost/rss", feed.getLink(), "RSS Feed Link stimmt nicht überein.");

        List<SyndEntry> entries = feed.getEntries();
        assertNotNull(entries, "RSS Einträge sollten nicht null sein.");
        assertEquals(2, entries.size(), "Anzahl der RSS Einträge stimmt nicht.");

        // Ersten Eintrag prüfen
        SyndEntry entry1 = entries.get(0);
        assertEquals("RSS Item 1", entry1.getTitle());
        assertEquals("Description for RSS item 1", entry1.getDescription().getValue());
        assertEquals("http://localhost/rss/item1", entry1.getLink());

        // Zweiten Eintrag prüfen
        SyndEntry entry2 = entries.get(1);
        assertEquals("RSS Item 2", entry2.getTitle());
        assertEquals("Description for RSS item 2", entry2.getDescription().getValue());
        assertEquals("http://localhost/rss/item2", entry2.getLink());
    }

    @Test
    void testFetchAndParseAtomFeed() throws Exception {
        // 1. Atom Feed abrufen
        String atomXml = RestAssured.when()
                .get("/test-feeds/atom")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_XML)
                .extract().body().asString();

        assertNotNull(atomXml, "Atom XML sollte nicht null sein.");
        assertTrue(atomXml.contains("<title>Test Atom Feed</title>"), "Atom XML Titel nicht gefunden.");

        // 2. Atom Feed parsen mit Rome
        SyndFeedInput input = new SyndFeedInput();
        // XmlReader ist oft besser, um Encoding-Probleme zu vermeiden, falls der Server kein UTF-8 sendet
        // oder das XML-Encoding-Deklaration fehlt/falsch ist.
        // Für StringReader hier:
        SyndFeed feed = input.build(new StringReader(atomXml));
        // Alternative mit XmlReader, falls du einen InputStream hättest:
        // SyndFeed feed = input.build(new XmlReader(inputStream));


        // 3. Daten prüfen
        assertNotNull(feed, "Geparster Atom Feed sollte nicht null sein.");
        assertEquals("Test Atom Feed", feed.getTitle(), "Atom Feed Titel stimmt nicht überein.");
        assertEquals("http://localhost/atom", feed.getLink(), "Atom Feed Link stimmt nicht überein."); // Rome holt den ersten Link, wenn mehrere vorhanden sind
        assertEquals("Test Author", feed.getAuthors().getFirst().getName(), "Atom Feed Author stimmt nicht überein."); // Rome holt den ersten Autor

        List<SyndEntry> entries = feed.getEntries();
        assertNotNull(entries, "Atom Einträge sollten nicht null sein.");
        assertEquals(2, entries.size(), "Anzahl der Atom Einträge stimmt nicht.");

        // Ersten Eintrag prüfen
        SyndEntry entry1 = entries.get(0);
        assertEquals("Atom Entry 1", entry1.getTitle());
        // Für Atom ist der Inhalt oft in entry.getContents() oder entry.getDescription() (was oft <summary> ist)
        // Rome mappt <summary> auf description.value
        assertNotNull(entry1.getDescription(), "Atom Entry 1 Beschreibung (summary) nicht gefunden");
        assertEquals("Summary for Atom entry 1", entry1.getDescription().getValue());
        assertEquals("http://localhost/atom/entry1", entry1.getLink());

        // Zweiten Eintrag prüfen
        SyndEntry entry2 = entries.get(1);
        assertEquals("Atom Entry 2", entry2.getTitle());
        assertNotNull(entry2.getDescription(), "Atom Entry 2 Beschreibung (summary) nicht gefunden");
        assertEquals("Summary for Atom entry 2", entry2.getDescription().getValue());
        assertEquals("http://localhost/atom/entry2", entry2.getLink());
    }
}
