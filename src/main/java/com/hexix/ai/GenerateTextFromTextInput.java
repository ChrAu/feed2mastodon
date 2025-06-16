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

    @ConfigProperty(name = "gemini.model")
    String geminiModel;

    public String getAiMessage(String initPost){
        try(Client client = Client.builder().apiKey(accessToken).build()) {
            final PromptEntity prompt = PromptEntity.findLatest();
            GenerateContentResponse response =
                    client.models.generateContent(geminiModel, String.format("%s \n\n'%s'",prompt.prompt, initPost), null);
            LOG.info("Input message:" + initPost);
            LOG.info("Generated message: " + response.text());
            return response.text();
        }
    }
}
