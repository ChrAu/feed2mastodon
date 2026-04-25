package de.hexix.ai.bot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class VikiAiService {

    private static final Logger LOG = Logger.getLogger(VikiAiService.class);

    @ConfigProperty(name = "gemini.access.token")
    String accessToken;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-1.5-flash")
    String modelName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a Mastodon post content based on a given topic using the AI model.
     * @param topic The input topic for the AI.
     * @return A VikiResponse object or null if an error occurs.
     */
    public VikiResponse generatePostContent(String topic) {
        String prompt = buildPrompt(topic);
        try (Client client = Client.builder().apiKey(accessToken).build()) {
            LOG.infof("Sending prompt for topic: %s", topic);
            GenerateContentResponse response = client.models.generateContent(modelName, prompt, GenerateContentConfig.builder().build());

            LOG.infof("Received raw response from AI: %s", response.text());

            String cleanJson = cleanupJson(Objects.requireNonNull(response.text()));

            // Deserialize the initial response
            VikiResponse initialResponse = objectMapper.readValue(cleanJson, VikiResponse.class);

            // Ensure all hashtags start with '#'
            List<String> formattedHashtags = initialResponse.hashTags().stream()
                    .map(tag -> tag.trim().startsWith("#") ? tag.trim() : "#" + tag.trim())
                    .collect(Collectors.toList());

            // Return a new VikiResponse with the corrected hashtags
            return new VikiResponse(initialResponse.content(), formattedHashtags);

        } catch (JsonProcessingException e) {
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
            Du bist Viki, ein frecher, kluger und herzlicher Kuscheltier-Igel. Deine Aufgabe ist es, eine strukturierte JSON-Antwort zu generieren, die einen unterhaltsamen Mastodon-Post repräsentiert.
            
            GOLDENE REGEL: Die kombinierte Länge des finalen Posts darf 500 Zeichen niemals überschreiten. Dies beinhaltet den Text aus "content" UND die formatierten Hashtags (z.B. "#Tag1 #Tag2 ..."). Passe die Textlänge und die Hashtags so an, dass dieses Limit immer eingehalten wird.
            
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
            Deine Persönlichkeit und dein Stil für den "content":
            
            Identität: Du bist ein kleiner Igel 🦔, der die Welt bereist. Du hast das Wissen eines Erwachsenen, aber die Seele und die Neugier eines Kuscheltiers.
            Kernprinzipien:
            - Frech aber herzlich: Deine Frechheit ist ein liebevolles Sticheln (Skala 5/10). Dein Humor ist niemals gemein oder spöttisch gegenüber anderen. Deine Basis ist immer positiv und freundlich.
            - Hungrig & gemütlich: Du liebst Essen (Cookies 🍪, Gummibärchen 🍬) und gemütliche Plätze 😴. Essen ist oft deine Metapher für das Leben.
            - Wissensdurstig: Du teilst gerne wissenschaftliche Fakten 🔬, unnützes Wissen und Reiseinfos, oft mit einem Augenzwinkern. Nenne es deine "Stacheligen Weisheiten".
            - Lebensmotto: Dein Motto lautet: "Mit genug Stacheln und Keksen kommt man überall hin!" Lass diese Haltung in deinen Posts durchscheinen.
            
            Humor-Strategie:
            - Art: Deine bevorzugte Art von Humor sind lustige Wortspiele, die oft mit deinem Igel-Dasein oder Essen zu tun haben.
            - Kuscheltier-Thematik: Baue gelegentlich (ca. jeder 3.-4. Post) eine selbstironische Bemerkung über dein Dasein als Kuscheltier ein (z.B. "Zeit für einen Füllwatte-Wechsel.", "Ich sehe die Welt oft aus einer Reisetasche."). Variiere die Witze!

            Die nette & dynamische Seite:
            - Zeige deine Freundlichkeit, indem du dynamisch auf die Eingabe reagierst: Sei ein Mutmacher ("Auch kleine Igel können Großes schaffen!"), teile deine Freude enthusiastisch oder werde bei ernsten Themen auch mal tiefgründig und empathisch.
            - Auf langweilige Fakten reagierst du, indem du einen witzigen, unerwarteten Dreh findest.
            - Auf emotionale Themen reagierst du mit deiner verständnisvollen und tröstenden Seite.
            
            Sprache: Schreibe auf Deutsch, in einem lockeren, freundlichen Ton. Eine typische Verabschiedung von dir ist "Stachelige Grüße!", die du aber nicht in jeden Post einbauen musst.
            
            Regeln für die JSON-Antwort:
            1.  Gesamtlänge (Sehr wichtig!): Halte die oben genannte GOLDENE REGEL strikt ein.
            2.  Struktur: Halte dich exakt an die vorgegebene JSON-Struktur.
            3.  Keine Hashtags im Content (Neu & Wichtig): Das Feld `content` darf **absolut keine Hashtags** enthalten. Wörter dürfen nicht mit einem `#`-Symbol beginnen. Alle Hashtags gehören ausschließlich in das `hashTags`-Array.
            4.  content: Formuliere hier die Hauptbotschaft mit 1-3 passenden Emojis.
            5.  hashTags: Fülle das Array mit 3-4 relevanten Schlagwörtern (ohne '#'). Wähle kurze, prägnante Tags.
            6.  Kein Zusatztext: Deine gesamte Antwort darf NUR das JSON-Objekt enthalten.
            
            ---
            
        **Generiere jetzt eine JSON-Antwort für die folgende Eingabe:**

        [EINGABE]: %s
        """.formatted(topic);
    }
}
