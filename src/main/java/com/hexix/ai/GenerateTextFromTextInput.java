package com.hexix.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HarmBlockThreshold;
import com.google.genai.types.HarmCategory;
import com.google.genai.types.SafetySetting;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;


@ApplicationScoped
public class GenerateTextFromTextInput {

    final Logger LOG = Logger.getLogger(this.getClass());


    @ConfigProperty(name = "gemini.access.token")
    String accessToken;




    @Transactional
    public String getAiMessage(String geminiModel, String initPost){
        try(Client client = Client.builder().apiKey(accessToken).build()) {
            final PromptEntity prompt = PromptEntity.findLatest();


            final String sendPrompt = String.format("%s \n\n'%s'", prompt.prompt, initPost);

            final GeminiRequestEntity geminiRequestEntity = new GeminiRequestEntity();
            geminiRequestEntity.setModel(geminiModel);
            geminiRequestEntity.setText(sendPrompt);
            geminiRequestEntity.persist();

            final List<SafetySetting> safetySettings = List.of(
                    SafetySetting.builder().category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH).threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE).build(),
                    SafetySetting.builder().category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT).threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE).build());

            GenerateContentResponse response =
                    client.models.generateContent(geminiModel, sendPrompt, GenerateContentConfig.builder().safetySettings(safetySettings).maxOutputTokens(500).build());
            LOG.info("Input message:" + initPost);
            LOG.info("Generated message: " + response.text());
            return response.text();
        }
    }
}
