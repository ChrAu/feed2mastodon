package com.hexix.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;


@ApplicationScoped
public class GenerateTextFromTextInput {

    final Logger LOG = Logger.getLogger(this.getClass());


    @ConfigProperty(name = "gemini.access.token")
    String accessToken;

    public String getAiMessage(String initPost){
        try(Client client = Client.builder().apiKey(accessToken).build()) {

            GenerateContentResponse response =
                    client.models.generateContent("gemini-2.0-flash", String.format("Erstelle einen Mastodon Post inkl. interessanter Hashtags aus (kein markdown link erstellen) dem Text: '%s'", initPost), null);
            LOG.info("Input message:" + initPost);
            LOG.info("Generated message: " + response.text());
            return response.text();
        }
    }
}
