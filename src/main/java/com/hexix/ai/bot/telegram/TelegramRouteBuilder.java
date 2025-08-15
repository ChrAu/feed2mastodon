package com.hexix.ai.bot.telegram;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TelegramRouteBuilder extends RouteBuilder {

    @ConfigProperty(name = "telegram.bot.token")
    String telegramBotToken;

    @Inject
    MessageProcessor messageProcessor;

    @Override
    public void configure() throws Exception {

        // Exception Handler für bessere Fehlerbehandlung
        onException(Exception.class)
                .handled(true)
                .log("Fehler beim Verarbeiten der Nachricht: ${exception.message}")
                .setBody(constant("❌ Entschuldigung, es ist ein Fehler aufgetreten!"))
                .to("telegram:bots?authorizationToken=" + telegramBotToken);

        // Hauptroute: Nachrichten empfangen, verarbeiten und zurücksenden
        from("telegram:bots?authorizationToken=" + telegramBotToken)
                .routeId("telegram-bot-route")
                .log("Neue Telegram Nachricht empfangen: ${body}")
                .filter(body().isNotNull())
                .process(messageProcessor)
                .log("Sende Antwort: ${body}")
                .to("telegram:bots?authorizationToken=" + telegramBotToken);

        // Health Check Route (optional)
        from("timer:healthCheck?period=300000&delay=10000") // Alle 5 Minuten
                .routeId("health-check-route")
                .log("Bot läuft... Token: " + telegramBotToken.substring(0, 10) + "...");
    }
}
