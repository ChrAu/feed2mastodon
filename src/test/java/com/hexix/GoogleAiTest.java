package com.hexix;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerationConfig;
import com.google.genai.types.HarmBlockThreshold;
import com.google.genai.types.HarmCategory;
import com.google.genai.types.SafetySetting;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

@QuarkusTest
public class GoogleAiTest {


    final Logger LOG = Logger.getLogger(this.getClass());


    @ConfigProperty(name = "gemini.access.token")
    String accessToken;

    @ConfigProperty(name = "gemini.model")
    String geminiModel;


    @Test
    public void simpleQuestionTest() {

        try (Client client = Client.builder().apiKey(accessToken).build()) {

            final GenerationConfig config = GenerationConfig.builder().responseMimeType("application/json").maxOutputTokens(256).build();
            String prompt = """
                    Erstelle einen kurzen Social-Media-Post zum Thema "Die Zukunft der künstlichen Intelligenz".
                    Fasse dich sehr kurz.
                    Der Post sollte einen informativen Inhalt, zwei relevante Weblinks und drei passende Hashtags enthalten.
                    Bitte gib die Antwort ausschließlich als JSON-Objekt zurück, das dem folgenden Schema entspricht:
                    {
                      "content": "string",
                      "links": ["string", "string"],
                      "hashTags": ["string", "string", "string"]
                    }
                    """;
            final List<SafetySetting> safetySettings = List.of(
                    SafetySetting.builder().category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH).threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE).build(),
                    SafetySetting.builder().category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT).threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE).build());

            GenerateContentResponse response = client.models.generateContent(geminiModel, prompt, GenerateContentConfig.builder().safetySettings(safetySettings).build());
            LOG.info("Input message:" + prompt);
            LOG.info("Generated message: " + response.text());
            Assertions.assertNotNull(response.text());
            Assertions.assertTrue(response.text().contains("\"content\":"));
            Assertions.assertTrue(response.text().contains("\"links\":"));
            Assertions.assertTrue(response.text().contains("\"hashTags\""));
        }
    }
}
