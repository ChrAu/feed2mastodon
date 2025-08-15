package com.hexix.ai.bot.telegram;

import com.hexix.JsoupParser;
import com.hexix.ai.bot.VikiAiService;
import com.hexix.ai.bot.VikiResponse;
import com.hexix.mastodon.PublicMastodonPostEntity;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.MastodonClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.telegram.model.IncomingCallbackQuery;
import org.apache.camel.component.telegram.model.IncomingMessage;
import org.apache.camel.component.telegram.model.InlineKeyboardButton;
import org.apache.camel.component.telegram.model.InlineKeyboardMarkup;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class MessageProcessor implements Processor {

    @RestClient
    @Inject
    MastodonClient mastodonClient;

    @Inject
    VikiAiService vikiAiService;

    @ConfigProperty(name = "mastodon.access.token")
    String accessToken;

    final static Logger LOG = Logger.getLogger(JsoupParser.class);

    private final Map<String, ChatState> chatStates = new ConcurrentHashMap<>();

    private static class ChatState {
        public String originalText;
        ConversationStep step;
        Object data;

        ChatState(ConversationStep step) {
            this.step = step;
        }
    }

    private enum ConversationStep {
        KLEIN_VIKI, AWAITING_URL
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // KORREKTUR: Wir prüfen den Typ des Bodys, um zwischen
        // einer normalen Nachricht und einem Button-Klick (CallbackQuery) zu unterscheiden.
        Object body = exchange.getIn().getBody();

        if (body instanceof IncomingCallbackQuery callbackQuery) {
            // Dies ist ein Button-Klick (CallbackQuery)
            LOG.info("Verarbeite IncomingCallbackQuery...");
            String chatId = callbackQuery.getMessage().getChat().getId().toString();
            String callbackQueryId = callbackQuery.getId();
            String data = callbackQuery.getData(); // Daten direkt aus dem Callback-Objekt holen
            handleCallbackQuery(exchange, chatId, callbackQueryId, data);

        } else if (body instanceof IncomingMessage incomingMessage) {
            // Dies ist eine reguläre Textnachricht
            LOG.info("Verarbeite IncomingMessage...");
            String chatId = incomingMessage.getChat().getId().toString();
            handleTextMessage(exchange, chatId, incomingMessage);

        } else {
            LOG.warnf("Unbekannter Nachrichtentyp empfangen: {}", body != null ? body.getClass().getName() : "null");
        }
    }


    private void handleCallbackQuery(Exchange exchange, String chatId, String callbackQueryId, String callbackData) {
        LOG.infof("Callback Query empfangen - Chat: {}, Data: {}", chatId, callbackData);



        ChatState currentState = chatStates.get(chatId);

        if(currentState.step == ConversationStep.KLEIN_VIKI) {
            handleVikiCallbackQuery(exchange, chatId, callbackQueryId, callbackData);
        }else if(currentState.step == ConversationStep.AWAITING_URL) {
            handleNegativCallbackQuery(exchange, chatId, callbackQueryId, callbackData);
        }



    }

    private void handleVikiCallbackQuery(Exchange exchange, String chatId, String callbackQueryId, String callbackData){
        String responseMessage = "Unbekannter Fehler aufgetreten";

        //todo Mastodon Post senden

        ChatState currentState = chatStates.get(chatId);
        final VikiResponse data = (VikiResponse) currentState.data;
        switch (callbackData) {
            case "viki_no":
                handleVikiInput(exchange, chatId, currentState.originalText);
                return;
            case "viki_yes":
                responseMessage = data.content() + " - wurde gepostet.";
                break;

            case "back_to_main":
                responseMessage = "🔙 Zurück zum Hauptmenü. Sende /start um zu erfahren was ich für dich machen kann.";
                break;


        }


        // Antwort-Nachricht setzen
        OutgoingTextMessage outgoingMessage = new OutgoingTextMessage();

        outgoingMessage.setText(responseMessage);
        outgoingMessage.setChatId(chatId);

        exchange.getIn().setBody(outgoingMessage);

        // Callback Query bestätigen (entfernt das "Loading" auf dem Button)
        exchange.getIn().setHeader("CamelTelegramAnswerCallbackQueryId", callbackQueryId);
        exchange.getIn().setHeader("CamelTelegramAnswerCallbackQueryText", "Auswahl verarbeitet!");
        chatStates.remove(chatId);
    }

    private void handleNegativCallbackQuery(Exchange exchange, String chatId, String callbackQueryId, String callbackData){
        String responseMessage;
        ChatState currentState = chatStates.get(chatId);
        String urlInfo = "";
        if (currentState != null && currentState.data instanceof String) {
            urlInfo = " für die URL: " + currentState.data;
        }

        double weight = 1;

        switch (callbackData) {
            case "negative_1":
                responseMessage = "😞 Du hast 'Sehr schlecht' ausgewählt" + urlInfo + ". Das tut mir leid zu hören.";
                weight = 5;
                break;
            case "negative_2":
                responseMessage = "😔 Du hast 'Schlecht' gewählt" + urlInfo + ". Hoffentlich wird es bald besser!";
                weight = 3;
                break;
            case "negative_3":
                responseMessage = "😐 Du hast 'Nicht so gut' gewählt" + urlInfo + ". Vielleicht kann ich dir helfen?";
                weight = 2;
                break;
            case "negative_4":
                responseMessage = "🤔 Du hast 'Könnte besser sein' gewählt" + urlInfo + ". Was bereitet dir Sorgen?";
                weight = 1;
                break;
            case "back_to_main":
                responseMessage = "🔙 Zurück zum Hauptmenü. Sende /negativ für die negative Bewertung.";
                weight = 0;
                break;
            default:
                responseMessage = "❓ Unbekannte Auswahl: " + callbackData;
                weight = 0;
        }



        chatStates.remove(chatId);

        if(weight > 0){
            try{
                final MastodonDtos.MastodonSearchResult search = mastodonClient.search("Bearer " + accessToken, String.valueOf(currentState.data), true);
                final Optional<MastodonDtos.MastodonStatus> status = search.statuses().stream().findFirst();

                if(status.isPresent()){
                    unBoost(status.get().id(), weight, false);
                }
            }catch (Exception e){
                responseMessage = "Es ist ein Fehler aufgetreten, die URL wird nicht als negativ bewertet";
                LOG.error("Fehler beim Laden des Status", e);
            }



        }

        // Antwort-Nachricht setzen
        OutgoingTextMessage outgoingMessage = new OutgoingTextMessage();
        outgoingMessage.setText(responseMessage);
        outgoingMessage.setChatId(chatId);

        exchange.getIn().setBody(outgoingMessage);

        // Callback Query bestätigen (entfernt das "Loading" auf dem Button)
        exchange.getIn().setHeader("CamelTelegramAnswerCallbackQueryId", callbackQueryId);
        exchange.getIn().setHeader("CamelTelegramAnswerCallbackQueryText", "Auswahl verarbeitet!");
    }

    private void handleTextMessage(Exchange exchange, String chatId, IncomingMessage incomingMessage) {
        String text = incomingMessage.getText();
        if (text == null || text.trim().isEmpty()) {
            exchange.getMessage().setBody("Du hast mir eine leere Nachricht geschickt! 🤔");
            exchange.getMessage().setHeader("CamelTelegramChatId", chatId);
            return;
        }

        if (text.trim().equals("/start")){
            chatStates.remove(chatId);
        }

        ChatState currentState = chatStates.get(chatId);
        if (currentState != null && currentState.step == ConversationStep.AWAITING_URL) {
            handleUrlInput(exchange, chatId, text);
            return;
        }else if(currentState != null && currentState.step == ConversationStep.KLEIN_VIKI){
            handleVikiInput(exchange, chatId, text);
            return;
        }

        if (text.trim().equals("/negativ")) {
            startNegativeFlow(exchange, chatId);
        }else if(text.trim().equals("/klein_Viki")){
            startVikiFlow(exchange, chatId);
        }else if(text.trim().equals("/clear")){
            clearFlow(exchange, chatId);
        }else if (text.trim().equals("/start")) {
            createStartMessage(exchange, chatId);
        } else if (text.trim().equals("/help")) {
            createHelpMessage(exchange, chatId);
        } else {
            String responseMessage = processMessage(text);
            exchange.getMessage().setBody(responseMessage);
            exchange.getMessage().setHeader("CamelTelegramChatId", chatId);
        }
    }

    private void clearFlow(final Exchange exchange, final String chatId) {
        chatStates.remove(chatId);
        String messageText = "Der State wurde gelöscht.";
        exchange.getMessage().setBody(messageText);
        exchange.getMessage().setHeader("CamelTelegramChatId", chatId);
        LOG.infof("Warte auf URL von Chat {}", chatId);
    }

    private void startNegativeFlow(Exchange exchange, String chatId) {
        chatStates.put(chatId, new ChatState(ConversationStep.AWAITING_URL));

        String messageText = "Bitte sende mir die URL, die du bewerten möchtest.";
        exchange.getMessage().setBody(messageText);
        exchange.getMessage().setHeader("CamelTelegramChatId", chatId);
        LOG.infof("Warte auf URL von Chat {}", chatId);
    }

    private void startVikiFlow(Exchange exchange, String chatId){
        chatStates.put(chatId, new ChatState(ConversationStep.KLEIN_VIKI));

        String messageText = "Bitte sende mir ein Thema, damit klein Viki dazu einen Beitrag erstelle kann.";
        exchange.getMessage().setBody(messageText);
        exchange.getMessage().setHeader("CamelTelegramChatId", chatId);
        LOG.infof("Warte auf Thema von Chat {}", chatId);
    }


    private void handleUrlInput(Exchange exchange, String chatId, String text) {
        try {
            new URL(text);

            ChatState currentState = chatStates.get(chatId);
            currentState.data = text;

            LOG.infof("Gültige URL '{}' von Chat {} erhalten.", text, chatId);

            createNegativeKeyboard(exchange, chatId);

        } catch (MalformedURLException e) {
            LOG.warnf("Ungültige URL '{}' von Chat {} erhalten.", text, chatId);
            String errorMessage = "Das scheint keine gültige URL zu sein. Bitte versuche es erneut (z.B. https://www.beispiel.de).";
            exchange.getMessage().setBody(errorMessage);
            exchange.getMessage().setHeader("CamelTelegramChatId", chatId);
        }
    }

    private void handleVikiInput(Exchange exchange, String chatId, String text) {
        ChatState currentState = chatStates.get(chatId);


        LOG.infof("Text '{}' von Chat {} erhalten.", text, chatId);

        final VikiResponse vikiResponse = vikiAiService.generatePostContent(text);
        currentState.data = vikiResponse;
        currentState.originalText = text;
        LOG.infof("Generierte Nachricht: {}", vikiResponse);

        createVikiKeyboard(exchange, chatId, vikiResponse.content());
    }


    private void createNegativeKeyboard(Exchange exchange, String chatId) {
        String messageText = "😔 Du möchtest eine negative Bewertung abgeben?\nWähle aus, wie schlecht es dir geht:";

        InlineKeyboardButton n1 = InlineKeyboardButton.builder()
                .text("😫 Sehr schlecht").callbackData("negative_1").build();
        InlineKeyboardButton n2 = InlineKeyboardButton.builder()
                .text("😞 Schlecht").callbackData("negative_2").build();
        InlineKeyboardButton n3 = InlineKeyboardButton.builder()
                .text("😐 Nicht so gut").callbackData("negative_3").build();
        InlineKeyboardButton n4 = InlineKeyboardButton.builder()
                .text("🤔 Könnte besser sein").callbackData("negative_4").build();
        InlineKeyboardButton back = InlineKeyboardButton.builder()
                .text("🔙 Abbrechen").callbackData("back_to_main").build();

        InlineKeyboardMarkup inlineKeyboard =
                InlineKeyboardMarkup.builder()
                        .addRow(Arrays.asList(n4, n3))
                        .addRow(Arrays.asList(n2, n1))
                        .addRow(Collections.singletonList(back))
                        .build();

        OutgoingTextMessage outgoingMessage = new OutgoingTextMessage();
        outgoingMessage.setText(messageText);
        outgoingMessage.setReplyMarkup(inlineKeyboard);
        outgoingMessage.setChatId(chatId);

        exchange.getIn().setBody(outgoingMessage);

        LOG.infof("Inline Keyboard für Chat {} erstellt", chatId);
    }

    private void createVikiKeyboard(Exchange exchange, String chatId, String content){
        String messageText = "Ich habe dir folgenden Post erstellt, bist da damit einverstanden?\n\n" + content;
        InlineKeyboardButton vYes = InlineKeyboardButton.builder()
                .text("Ja, posten").callbackData("viki_yes").build();

        InlineKeyboardButton vNo = InlineKeyboardButton.builder()
                .text("Nein, neu generieren").callbackData("viki_no").build();
        InlineKeyboardButton cancel = InlineKeyboardButton.builder()
                .text("Abbrechen").callbackData("back_to_main").build();

        final InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .addRow(Arrays.asList(vYes, vNo))
                .addRow(Collections.singletonList(cancel))
                .build();

        OutgoingTextMessage outgoingMessage = new OutgoingTextMessage();
        outgoingMessage.setText(messageText);
        outgoingMessage.setReplyMarkup(keyboard);
        outgoingMessage.setChatId(chatId);

        exchange.getIn().setBody(outgoingMessage);

        LOG.infof("Inline Keyboard für Chat {} erstellt", chatId);

    }

    private void createStartMessage(Exchange exchange, String chatId) {
        String startMessage = """
            🤖 Willkommen beim Telegram Bot!
            
            Verfügbare Kommandos:
            /negativ - Negative Bewertung mit Tastatur
            /klein_Viki - Erstellt einen Mastodon Post von klein Viki zum übergebenen Post
            /help - Hilfe anzeigen
            
            Du kannst mir auch einfach eine Nachricht schreiben!
            """;

        exchange.getMessage().setBody(startMessage);
        exchange.getMessage().setHeader("CamelTelegramChatId", chatId);
    }

    private void createHelpMessage(Exchange exchange, String chatId) {
        String helpMessage = """
            📚 Hilfe - Telegram Bot
            
            🔹 Sende /negativ, um eine URL zu bewerten.
            🔹 Sende /klein_Viki, um einen Mastodon Post von klein Viki zum übergebenen Thema zu posten
            🔹 Sende /clear, um deinen State zurück zu setzten.
            🔹 Sende /start für das Hauptmenü.
            🔹 Schreibe mir einfach eine Nachricht für ein Echo.
            
            💡 Tipp: Die Buttons in der Tastatur sind interaktiv!
            """;

        exchange.getMessage().setBody(helpMessage);
        exchange.getMessage().setHeader("CamelTelegramChatId", chatId);
    }

    private String processMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        if (message.toLowerCase().contains("hallo") || message.toLowerCase().contains("hi")) {
            return String.format("👋 Hallo! Du hast mir um %s geschrieben: \"%s\"", timestamp, message);
        } else if (message.toLowerCase().contains("wie geht")) {
            return String.format("🤖 Mir geht es gut, danke der Nachfrage! Du fragtest um %s: \"%s\"", timestamp, message);
        } else if (message.toLowerCase().contains("zeit")) {
            return String.format("⏰ Es ist jetzt %s. Deine Nachricht war: \"%s\"", timestamp, message);
        } else if (message.toLowerCase().contains("danke")) {
            return String.format("😊 Gern geschehen! Du sagtest um %s: \"%s\"", timestamp, message);
        } else {
            return String.format("📨 Echo um %s: \"%s\"", timestamp, message);
        }
    }

    @Transactional
    void unBoost(final String replyId, final Double negativeWeight, boolean noUrl) {
        PublicMastodonPostEntity post = PublicMastodonPostEntity.findByMastodonId(replyId);

        if(post == null){
            MastodonDtos.MastodonStatus status = mastodonClient.getStatus(replyId, "Bearer " + accessToken);
            post = getPublicMastodonPostEntity(status, noUrl);
        }else{
            if(noUrl){
                post.setUrlText(null);
                post.removeEmbeddingVektor();
            }
        }

        try{
            mastodonClient.unBoostStatus(replyId, "Bearer " + accessToken);
        }catch (Exception e){
            LOG.errorf(e,"Fehler unboost status Id: %s", replyId);
        }

        post.setNoURL(noUrl);


        post.setNegativeWeight(negativeWeight);

        // Zuerst versuchen, als MastodonStatus zu parsen (für 'update' oder 'status.update' Events)

    }

    private static PublicMastodonPostEntity getPublicMastodonPostEntity(final MastodonDtos.MastodonStatus status, boolean noUrl) {
        final PublicMastodonPostEntity post = new PublicMastodonPostEntity();
        post.setMastodonId(status.id());
        post.setStatusOriginalUrl(status.url());

        final String text = Jsoup.parse(status.content()).text();

        post.setNoURL(noUrl);

        LOG.infof("Empfangener Status (ID: %s, Account: %s, Inhalt: \"%s\", URL: %s)\n",
                status.id(), status.account().username(), text.substring(0, Math.min(20, text.length())) + "...", post.getStatusOriginalUrl());

        post.persist();
        return post;
    }

}
