package com.hexix.ai.bot.telegram;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Path("/health")
@ApplicationScoped
public class BotHealthCheck {

    @ConfigProperty(name = "telegram.bot.token")
    String telegramBotToken;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        health.put("bot", "Telegram Bot is running");
        health.put("token_configured", !telegramBotToken.equals("YOUR_TELEGRAM_BOT_TOKEN_HERE"));
        return health;
    }

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Quarkus Telegram Bot");
        info.put("version", "1.0.0");
        info.put("framework", "Quarkus + Apache Camel");
        info.put("description", "Echo Bot that processes and returns messages");
        return info;
    }
}
