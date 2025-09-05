package com.hexix.ai.bot;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.rmi.ServerException;
import java.util.stream.Collectors;

@QuarkusTest
class VikiAiServiceTest {


    @Inject
    VikiAiService vikiAiService;

    @Test
    @DisplayName("Should generate a valid VikiResponse for a given topic")
    public void testGeneratePostContent() {
        // 1. Definiere ein Test-Thema
        String topic = "Fakten über Tassen";

        // 2. Rufe den Service auf, der getestet werden soll
        try {
            VikiResponse response = vikiAiService.generatePostContent(topic);

            // 3. Gib das Ergebnis zur manuellen Überprüfung auf der Konsole aus
            System.out.println("--- AI Response Test ---");
            if (response != null) {
                System.out.println("Content: " + response.content());
                System.out.println("Hashtags: " + response.hashTags());

                // Formatiere den Post, um die Längenprüfung zu simulieren
                String hashtags = response.hashTags().stream()
                        .map(tag -> "#" + tag.replaceAll("\\s", ""))
                        .collect(Collectors.joining(" "));
                String finalPost = response.content() + "\n\n" + hashtags;

                System.out.println("\n--- Simulated Final Post ---");
                System.out.println(finalPost);
                System.out.println("Total length: " + finalPost.length() + " characters");
                System.out.println("--------------------------");

                // Überprüfe, ob die KI die Längenregel eingehalten hat
                Assertions.assertTrue(finalPost.length() <= 500, "The final post should not exceed 500 characters.");

            } else {
                System.out.println("Response from AI was null. Check logs for errors.");
            }
            System.out.println("------------------------");


            // 4. Automatische Überprüfungen (Assertions)
            //    Stellt sicher, dass die Antwort nicht null ist und die erwartete Struktur hat.
            Assertions.assertNotNull(response, "The response from the AI service should not be null.");
            Assertions.assertNotNull(response.content(), "The content of the response should not be null.");
            Assertions.assertFalse(response.content().isBlank(), "The content should not be blank.");
            Assertions.assertNotNull(response.hashTags(), "The hashtags list should not be null.");
            Assertions.assertFalse(response.hashTags().isEmpty(), "The hashtags list should not be empty.");
        }catch (Exception e){
            Assertions.assertTrue(e.getMessage().contains("The model is overloaded"), "Google KI ist gerade überlastet.");
            if(!(e instanceof ServerException)){
                throw new RuntimeException("Fehler beim Test", e);
            }

        }
    }

}
