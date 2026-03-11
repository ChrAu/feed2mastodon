package com.hexix;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class SpaRouteFilter {

    /**
     * Registriert einen Handler am Ende der Route-Kette.
     * Wenn bis dahin keine Route gematcht hat (404), prüfen wir, ob es sich um eine Frontend-Route handelt.
     */
    void init(@Observes Router router) {
        router.route().last().handler(this::handleFallback);
    }

    void handleFallback(RoutingContext rc) {
        // Wenn die Antwort bereits geschrieben wurde oder kein 404 ist, tun wir nichts.
        if (rc.response().ended()) {
            return;
        }

        // Wir gehen davon aus, dass wenn wir hier ankommen, keine Route gematcht hat (404).
        
        String path = rc.normalizedPath();

        // Ausschlussliste: Pfade, die ECHTE 404s sein sollen (API, statische Assets, etc.)
        boolean isApiOrAsset = path.startsWith("/api") ||
                               path.startsWith("/s/") || // Beachte den Slash, um /s nicht mit /start zu verwechseln
                               path.equals("/s") ||
                               path.startsWith("/health") ||
                               path.startsWith("/status") ||
                               path.startsWith("/themen") ||
                               path.startsWith("/q") ||   // Quarkus Dev UI etc.
                               path.contains(".");        // Dateien (z.B. .js, .css, .ico)

        if (!isApiOrAsset) {
            // Es ist eine Frontend-Route (z.B. /impressum) -> Reroute zu / (index.html)
            rc.reroute("/");
        } else {
            // Es ist ein echter 404 (z.B. fehlende API oder Datei)
            rc.next();
        }
    }
}