package com.hexix;

import com.google.genai.Client;
import com.google.genai.types.BatchJob;
import com.google.genai.types.BatchJobSource;
import com.google.genai.types.Content;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.CreateBatchJobConfig;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerationConfig;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.HarmBlockThreshold;
import com.google.genai.types.HarmCategory;
import com.google.genai.types.InlinedRequest;
import com.google.genai.types.Part;
import com.google.genai.types.SafetySetting;
import com.google.genai.types.Tool;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.FavouritesResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@QuarkusTest
public class GoogleAiTest {


    final Logger LOG = Logger.getLogger(this.getClass());


    @ConfigProperty(name = "gemini.access.token")
    String accessToken;

    @ConfigProperty(name = "gemini.model")
    String geminiModel;

    static Integer EMBEDDING_DIMENSION = 768;





    @Test
    public void embeddingTests(){
        try (Client client = Client.builder().apiKey(accessToken).build()) {

            System.out.println("Rufe Embeddings mit dem Google Gen AI SDK ab...");

            final EmbedContentResponse embedContentResponse = client.models.embedContent("gemini-embedding-001","Patchday: Adobe schützt After Effects & Co. vor möglichen Attacken\n" +
                    "\n" +
                    "Mehrere Adobe-Anwendungen sind unter anderem für DoS- und Schadcode-Attacken anfällig. Sicherheitsupdates schaffen Abhilfe. " +
                    "https://www.heise.de/news/Patchday-Adobe-schuetzt-After-Effects-Co-vor-moeglichen-Attacken-10479838.html?wt_mc=sm.red.ho.mastodon.mastodon.md_beitraege.md_beitraege&utm_source=mastodon" +
                    "#\n" +
                    "Adobe\n" +
                    "#\n" +
                    "IT\n" +
                    "#\n" +
                    "Patchday\n" +
                    "#\n" +
                    "Security\n" +
                    "#\n" +
                    "Sicherheitslücken\n" +
                    "#\n" +
                    "Updates\n" +
                    "#", EmbedContentConfig.builder().taskType("SEMANTIC_SIMILARITY").build());

            final List<ContentEmbedding> contentEmbeddings = embedContentResponse.embeddings().get();


            final List<ContentEmbedding> compareExample = client.models.embedContent("gemini-embedding-001", "Patchday: Adobe schützt After Effects & Co. vor möglichen Attacken\n" +
                    "\n" +
                    "Mehrere Adobe-Anwendungen sind unter anderem für DoS- und Schadcode-Attacken anfällig. Sicherheitsupdates schaffen Abhilfe.", EmbedContentConfig.builder().taskType("SEMANTIC_SIMILARITY").build()).embeddings().get();



            final double[] result1 = Arrays.stream(contentEmbeddings.getFirst().values().get().<Float>toArray(new Float[0])).mapToDouble(Float::doubleValue).toArray();

            final double[] result2 = Arrays.stream(compareExample.getFirst().values().get().<Float>toArray(new Float[0])).mapToDouble(Float::doubleValue).toArray();

            double[] userProfileVector = createProfileVector(List.of(result1));

            double similarityScore1 = getCosineSimilarity(userProfileVector, result2);
            System.out.println(similarityScore1);

            Assertions.assertTrue(similarityScore1 > 0.9, "Die Ähnlichkeit lieft bei unter 0,9");

        }


    }

    @Test
    public void simpleQuestionTest() {

        try (Client client = Client.builder().apiKey(accessToken).build()) {
            final List<SafetySetting> safetySettings = List.of(
                    SafetySetting.builder().category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH).threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE).build(),
                    SafetySetting.builder().category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT).threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE).build());
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


            GenerateContentResponse response = client.models.generateContent(geminiModel, prompt, GenerateContentConfig.builder().maxOutputTokens(256).safetySettings(safetySettings).build());
            LOG.info("Input message:" + prompt);
            LOG.info("Generated message: " + response.text());
            Assertions.assertNotNull(response.text());
            Assertions.assertTrue(response.text().contains("\"content\":"));
            Assertions.assertTrue(response.text().contains("\"links\":"));
            Assertions.assertTrue(response.text().contains("\"hashTags\""));
        }
    }

    @Test
    public void simpleQuestionOnlineTest() {

        try (Client client = Client.builder().apiKey(accessToken).build()) {

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

            GenerateContentResponse response = client.models.generateContent("gemini-2.0-flash", prompt, GenerateContentConfig.builder().responseMimeType("application/json").maxOutputTokens(256).tools(Tool.builder().googleSearch(GoogleSearch.builder().build()).build()).safetySettings(safetySettings).build());
            LOG.info("Input message:" + prompt);
            LOG.info("Generated message: " + response.text());
            Assertions.assertNotNull(response.text());
            Assertions.assertTrue(response.text().contains("\"content\":"));
            Assertions.assertTrue(response.text().contains("\"links\":"));
            Assertions.assertTrue(response.text().contains("\"hashTags\""));
        }
    }

    @Test
    public void new001EmbeddingsTest() {
        try(Client client = Client.builder().apiKey(accessToken).build()) {
            System.out.println("Rufe Embeddings mit dem Google Gen AI SDK ab...");

            final EmbedContentResponse embedContentResponse = client.models.embedContent("gemini-embedding-001","Patchday: Adobe schützt After Effects & Co. vor möglichen Attacken\n" +
                    "\n" +
                    "Mehrere Adobe-Anwendungen sind unter anderem für DoS- und Schadcode-Attacken anfällig. Sicherheitsupdates schaffen Abhilfe. " +
                    "https://www.heise.de/news/Patchday-Adobe-schuetzt-After-Effects-Co-vor-moeglichen-Attacken-10479838.html?wt_mc=sm.red.ho.mastodon.mastodon.md_beitraege.md_beitraege&utm_source=mastodon" +
                    "#\n" +
                    "Adobe\n" +
                    "#\n" +
                    "IT\n" +
                    "#\n" +
                    "Patchday\n" +
                    "#\n" +
                    "Security\n" +
                    "#\n" +
                    "Sicherheitslücken\n" +
                    "#\n" +
                    "Updates\n" +
                    "#", EmbedContentConfig.builder().taskType("SEMANTIC_SIMILARITY").build());

            final List<ContentEmbedding> contentEmbeddings = embedContentResponse.embeddings().get();


            final List<ContentEmbedding> compareExample = client.models.embedContent("gemini-embedding-001", "Patchday: Adobe schützt After Effects & Co. vor möglichen Attacken\n" +
                    "\n" +
                    "Mehrere Adobe-Anwendungen sind unter anderem für DoS- und Schadcode-Attacken anfällig. Sicherheitsupdates schaffen Abhilfe.", EmbedContentConfig.builder().taskType("SEMANTIC_SIMILARITY").build()).embeddings().get();



            final double[] result1 = Arrays.stream(contentEmbeddings.getFirst().values().get().<Float>toArray(new Float[0])).mapToDouble(Float::doubleValue).toArray();

            final double[] result2 = Arrays.stream(compareExample.getFirst().values().get().<Float>toArray(new Float[0])).mapToDouble(Float::doubleValue).toArray();

            double[] userProfileVector = createProfileVector(List.of(result1));

            double similarityScore1 = getCosineSimilarity(userProfileVector, result2);
            System.out.println(similarityScore1);

            Assertions.assertTrue(similarityScore1 > 0.9, "Die Ähnlichkeit lieft bei unter 0,9");

        }
    }


    @Test
    public void simpleEmbedding() {

        try (Client client = Client.builder().apiKey(accessToken).build()) {

            System.out.println("Rufe Embeddings mit dem Google Gen AI SDK ab...");


//            final double[] f1 = getContentEmbeddings(client, "Bericht über das Wachstum der deutschen Wirtschaft im letzten Quartal.");
            final double[] f1 = getContentEmbeddings(client, "Patchday: Adobe schützt After Effects & Co. vor möglichen Attacken\n" +
                    "\n" +
                    "Mehrere Adobe-Anwendungen sind unter anderem für DoS- und Schadcode-Attacken anfällig. Sicherheitsupdates schaffen Abhilfe. " +
                    "https://www.heise.de/news/Patchday-Adobe-schuetzt-After-Effects-Co-vor-moeglichen-Attacken-10479838.html?wt_mc=sm.red.ho.mastodon.mastodon.md_beitraege.md_beitraege&utm_source=mastodon" +
                    "#\n" +
                    "Adobe\n" +
                    "#\n" +
                    "IT\n" +
                    "#\n" +
                    "Patchday\n" +
                    "#\n" +
                    "Security\n" +
                    "#\n" +
                    "Sicherheitslücken\n" +
                    "#\n" +
                    "Updates\n" +
                    "#");

            final double[] f2 = getContentEmbeddings(client, "\uD83D\uDCDA Astrophysiker Wolfgang Priester vor 20 Jahren verstorben.\n" +
                    "\n" +
                    "• Pionier der Satellitenbahnberechnung.\n" +
                    "• Forschung zu Hochatmosphäre und Kosmos.\n" +
                    "• Begründer zweier Großteleskope. " +
                    "https://www.deutschlandfunk.de/sternzeit-9-juli-2025-wolfgang-priester-effelsberg-und-visionaere-kosmologie-100.html" +
                    "#\n" +
                    "Astrophysik\n" +
                    "#\n" +
                    "WolfgangPriester\n" +
                    "#\n" +
                    "Kosmologie\n" +
                    "#\n" +
                    "Effelsberg\n" +
                    "#");
            final double[] f3 = getContentEmbeddings(client, "Die neuesten Trends in der Automobilindustrie und E-Mobilität.");
            final double[] f4 = getContentEmbeddings(client, "\uD83D\uDCDA Sturzfluten in Texas und Ahrtal weisen Gemeinsamkeiten auf.\n" +
                    "\n" +
                    "• Gemeinsame Faktoren: Wetterlage, Gelände, Warnsysteme.\n" +
                    "• Lehren aus Katastrophen können Leben retten.");

            System.out.println("Embeddings erfolgreich erhalten.");
            System.out.println("----------------------------------------------------");

            System.out.println("Benutzerprofil wird aus gelikten Inhalten erstellt...");
            List<double[]> likedVectors = Arrays.asList(f1, f2);
            double[] userProfileVector = createProfileVector(likedVectors);
            System.out.println("Benutzerprofil-Vektor erfolgreich erstellt.");
            System.out.println("----------------------------------------------------");


            // --- Phase C: Filtern & Empfehlen ---
            double similarityScore1 = getCosineSimilarity(userProfileVector, f3);
            double similarityScore2 = getCosineSimilarity(userProfileVector, f4);

            System.out.printf("Ähnlichkeits-Score für Artikel 1 ('Automobilindustrie'): %.4f%n", similarityScore1);
            System.out.printf("Ähnlichkeits-Score für Artikel 2 ('Apfelkuchen'): %.4f%n", similarityScore2);
            System.out.println("----------------------------------------------------");

            if (similarityScore1 > similarityScore2) {
                System.out.println("Empfehlung: Der Artikel über die Automobilindustrie passt besser zu den Interessen des Benutzers.");
            } else {
                System.out.println("Empfehlung: Der Artikel über Apfelkuchen passt besser zu den Interessen des Benutzers.");
            }


        }
    }


    /**
     * Berechnet die Kosinus-Ähnlichkeit zwischen zwei Vektoren.
     * @param vectorA Der erste Vektor.
     * @param vectorB Der zweite Vektor.
     * @return Der Ähnlichkeits-Score.
     */
    public static double getCosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
        }
        return dotProduct;
    }

    private static double[]  getContentEmbeddings(final Client client, String text) {
        final EmbedContentResponse embedContentResponse = client.models.embedContent("models/text-embedding-004",text, EmbedContentConfig.builder().outputDimensionality(EMBEDDING_DIMENSION).build());
//                "Patchday: Adobe schützt After Effects & Co. vor möglichen Attacken\n" +
//                "\n" +
//                "Mehrere Adobe-Anwendungen sind unter anderem für DoS- und Schadcode-Attacken anfällig. Sicherheitsupdates schaffen Abhilfe. \n" +
//                "\n" +
//                "https://www.heise.de/news/Patchday-Adobe-schuetzt-After-Effects-Co-vor-moeglichen-Attacken-10479838.html?wt_mc=sm.red.ho.mastodon.mastodon.md_beitraege.md_beitraege&utm_source=mastodon" +
//                "#\n" +
//                "Adobe\n" +
//                "#\n" +
//                "IT\n" +
//                "#\n" +
//                "Patchday\n" +
//                "#\n" +
//                "Security\n" +
//                "#\n" +
//                "Sicherheitslücken\n" +
//                "#\n" +
//                "Updates\n" +
//                "#",

        final List<ContentEmbedding> contentEmbeddings = embedContentResponse.embeddings().get();

        final double[] result = Arrays.stream(contentEmbeddings.getFirst().values().get().<Float>toArray(new Float[0])).mapToDouble(Float::doubleValue).toArray();
        return result;
    }

    @Test
    public void testCreateVector(){
        final Random random = new Random();

        List<double[]> vectors = new LinkedList<>();

        for(int i = 0; i<10000;i++){
            double [] vector = new double[EMBEDDING_DIMENSION];
            for(int j = 0; j<vector.length;j++){
                vector[j] = random.nextFloat(2)-1;
            }
            vectors.add(vector);
        }

        final long start = System.currentTimeMillis();
        double[] profileVector = createProfileVector(vectors);
        System.out.println((System.currentTimeMillis() - start) + " ms: braucht die erstellung eines Profile mit " + vectors.size() + " Vektoren");


    }

    /**
     * Erstellt einen Profil-Vektor, indem der Durchschnitt mehrerer Vektoren gebildet wird.
     * @param vectors Eine Liste von Vektoren.
     * @return Der durchschnittliche, normalisierte Vektor.
     */
    public static double[] createProfileVector(List<double[]> vectors) {
        if (vectors == null || vectors.isEmpty() || vectors.stream().map(doubles -> doubles.length).count() > 1) {

            return new double[EMBEDDING_DIMENSION];
        }
        final int dimension = vectors.getFirst().length;

        double[] profileVector = new double[dimension];
        for (double[] vector : vectors) {
            for (int i = 0; i < dimension; i++) {
                profileVector[i] += vector[i];
            }
        }

        for (int i = 0; i < dimension; i++) {
            profileVector[i] /= vectors.size();
        }
        return normalize(profileVector);
    }

    /**
     * Normalisiert einen Vektor, sodass seine Länge 1 beträgt.
     * @param vector Der zu normalisierende Vektor.
     * @return Der normalisierte Vektor.
     */
    private static double[] normalize(double[] vector) {
        double magnitude = 0.0;
        for (double v : vector) {
            magnitude += v * v;
        }
        magnitude = Math.sqrt(magnitude);

        if (magnitude == 0) return vector;

        double[] normalizedVector = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalizedVector[i] = vector[i] / magnitude;
        }
        return normalizedVector;
    }
}
