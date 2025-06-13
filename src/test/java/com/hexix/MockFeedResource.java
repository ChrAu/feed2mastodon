package com.hexix;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/test-feeds")
public class MockFeedResource {

    public static final String RSS_FEED_XML = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <rss version="2.0">
            <channel>
              <title>Test RSS Feed</title>
              <link>http://localhost/rss</link>
              <description>This is a test RSS feed.</description>
              <item>
                <title>RSS Item 1</title>
                <link>http://localhost/rss/item1</link>
                <description>Description for RSS item 1</description>
                <pubDate>Mon, 01 Jan 2024 12:00:00 GMT</pubDate>
                <guid>http://localhost/rss/item1</guid>
              </item>
              <item>
                <title>RSS Item 2</title>
                <link>http://localhost/rss/item2</link>
                <description>Description for RSS item 2</description>
                <pubDate>Tue, 02 Jan 2024 12:00:00 GMT</pubDate>
                <guid>http://localhost/rss/item2</guid>
              </item>
            </channel>
            </rss>
            """;
    public static final String ATOM_FEED_XML = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Test Atom Feed</title>
              <link href="http://localhost/atom"/>
              <updated>2024-01-02T12:00:00Z</updated>
              <author>
                <name>Test Author</name>
              </author>
              <id>urn:uuid:some-unique-id-atom</id>
              <entry>
                <title>Atom Entry 1</title>
                <link href="http://localhost/atom/entry1"/>
                <id>urn:uuid:atom-entry-1</id>
                <updated>2024-01-01T12:00:00Z</updated>
                <summary>Summary for Atom entry 1</summary>
              </entry>
              <entry>
                <title>Atom Entry 2</title>
                <link href="http://localhost/atom/entry2"/>
                <id>urn:uuid:atom-entry-2</id>
                <updated>2024-01-02T12:00:00Z</updated>
                <summary>Summary for Atom entry 2</summary>
              </entry>
            </feed>
            """;


    @GET
    @Path("/rss")
    @Produces(MediaType.APPLICATION_XML)
    public Response getRssFeed() {
        return Response.ok(RSS_FEED_XML).build();
    }

    @GET
    @Path("/atom")
    @Produces(MediaType.APPLICATION_XML)
    public Response getAtomFeed() {
        return Response.ok(ATOM_FEED_XML).build();
    }

}
