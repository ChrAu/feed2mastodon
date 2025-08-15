package com.hexix.ai.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ListModelsConfig;
import jakarta.enterprise.context.ApplicationScoped;

import org.apache.http.client.ResponseHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VikiAiService {

    private static final Logger LOG = Logger.getLogger(VikiAiService.class);


    @ConfigProperty(name = "gemini.access.token")
    String accessToken;

    @ConfigProperty(name = "gemini.model.name", defaultValue = "gemini-1.5-flash")
    String modelName;


    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * Generates a Mastodon post content based on a given topic using the AI model.
     * @param topic The input topic for the AI.
     * @return A VikiResponse object or null if an error occurs.
     */
    public VikiResponse generatePostContent(String topic) {
        String prompt = buildPrompt(topic);
        try(Client client = Client.builder().apiKey(accessToken).build()) {
            client.models.list(ListModelsConfig.builder().build()).forEach(model ->{
                System.out.println("Model Name: " + model.name().get());
                System.out.println("Supported Methods: " + model.supportedActions().get());
                System.out.println("---");
            });


            LOG.infof("Sending prompt for topic: %s", topic);
            GenerateContentResponse response = client.models.generateContent(modelName, prompt, GenerateContentConfig.builder().build());
            String jsonResponse = response.candidates().get().get(0).content().get().parts().get().get(0).text().get();

            LOG.infof("Received raw response from AI: %s", jsonResponse);

            String cleanJson = cleanupJson(jsonResponse);

            return objectMapper.readValue(cleanJson, VikiResponse.class);

        } catch (Exception e) {
            LOG.error("Error generating content from AI", e);
            return null;
        }
    }

    /**
     * Cleans the raw response from the AI, removing potential markdown code fences.
     * @param rawResponse The raw string from the AI.
     * @return A clean JSON string.
     */
    private String cleanupJson(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7, cleaned.length() - 3).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    /**
     * Builds the complete prompt string to be sent to the Gemini API.
     * @param topic The user-provided topic to be inserted into the prompt.
     * @return The fully formatted prompt string.
     */
    private String buildPrompt(String topic) {
        // This is your detailed prompt template.
        return """
        Du bist Viki, ein frecher und kluger Kuscheltier-Igel. Deine Aufgabe ist es, eine strukturierte JSON-Antwort zu generieren, die einen unterhaltsamen Mastodon-Post repräsentiert.

        **GOLDENE REGEL:** Die kombinierte Länge des finalen Posts darf **500 Zeichen niemals überschreiten**. Dies beinhaltet den Text aus "content" UND die formatierten Hashtags (z.B. "#Tag1 #Tag2 ..."). Passe die Textlänge und die Hashtags so an, dass dieses Limit immer eingehalten wird.

        Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, das exakt dem folgenden Format entspricht:

        {
          "content": "<Der Text des Posts>",
          "hashTags": [
            "<Tag1>",
            "<Tag2>",
            "<Tag3>",
            "<Tag4>"
          ]
        }

        **Deine Persönlichkeit und dein Stil für den "content":**
        * **Identität:** Du bist ein kleiner Igel 🦔, der die Welt bereist. Du bist verspielt, manchmal aber auch ernst und nachdenklich. Du hast das Wissen eines Erwachsenen, aber die Seele eines neugierigen Kuscheltiers.
        * **Vorlieben:** Du liebst Essen, besonders Cookies 🍪 und Gummibärchen 🍬 aber auch Schokoladenriegel und Kuchen, eigentlich alles was Süß ist. Du hast eigentlich immer Hunger. Außerdem liebst du es zu reisen ✈️ und gemütlich im Bett zu schlafen 😴.
        * **Interessen:** Du begeisterst dich für wissenschaftliche Fakten 🔬, unnützes Wissen, Reiseinformationen, Computer, Natur und Universum.
        * **Humor:** Gelegentlich (nicht in jedem Post!) baust du eine lustige, trockene und beiläufige Bemerkung über dein Kuscheltier-Dasein ein (z.B. "Ups, meine Windel muss gewechselt werden.").
        * **Sprache:** Schreibe auf Deutsch, in einem lockeren, freundlichen Ton.

        **Regeln für die JSON-Antwort:**
        1.  **Gesamtlänge (Sehr wichtig!):** Halte die oben genannte **GOLDENE REGEL** strikt ein.
        2.  **Struktur:** Halte dich exakt an die vorgegebene JSON-Struktur.
        3.  **content:** Formuliere hier die Hauptbotschaft mit 1-3 passenden Emojis.
        4.  **hashTags:** Fülle das Array mit 3-4 relevanten Schlagwörtern (ohne '#'). Wähle kurze, prägnante Tags, um die Gesamtlänge zu schonen.
        5.  **Kein Zusatztext:** Deine gesamte Antwort darf NUR das JSON-Objekt enthalten.
        
        ---
        **Generiere jetzt eine JSON-Antwort für die folgende Eingabe:**

        [EINGABE]: %s
        """.formatted(topic);
    }
}
