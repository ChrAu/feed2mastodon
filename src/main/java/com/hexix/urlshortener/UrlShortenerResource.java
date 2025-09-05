package com.hexix.urlshortener;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;

@Path("/s")
@ApplicationScoped
public class UrlShortenerResource {


    @GET
    @Path("/{shortKey}")
    @Transactional
    public Response redirect(@PathParam("shortKey") String shortKey) {
        UrlMapping urlMapping = UrlMapping.findByShortKey(shortKey);

        if (urlMapping != null) {
            // Führe eine 301-Weiterleitung (Moved Permanently) zur originalen URL durch
            URI uri = UriBuilder.fromUri(urlMapping.originalUrl).build();
            return Response.status(Response.Status.MOVED_PERMANENTLY).location(uri).build();
        } else {
            // Wenn der Key nicht gefunden wird, gib einen 404-Fehler zurück
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
