package com.hexix.mastodon;

import com.google.genai.types.ContentEmbedding;
import com.hexix.JsoupParser;
import com.hexix.ai.GeminiRequestEntity;
import com.hexix.ai.GenerateEmbeddingTextInput;
import com.hexix.ai.OllamaRestClient;
import com.hexix.ai.dto.EmbeddingRequest;
import com.hexix.ai.dto.EmbeddingResponse;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.FavouritesService;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestScoped
public class StarredMastodonPosts {

    final Logger LOG = Logger.getLogger(this.getClass());


    @Inject
    FavouritesService favouritesService;

    @Inject
    GenerateEmbeddingTextInput generateEmbeddingTextInput;

    @Inject
    @RestClient
    OllamaRestClient ollamaRestClient;


    @Transactional
    public void collectNewStarredPosts(){
        final List<MastodonDtos.MastodonStatus> newFavourites = favouritesService.getNewFavourites();

        System.out.println("Neue Favourites: " + newFavourites.size());


        newFavourites.forEach(mastodonStatus -> favouritesService.createEmbedding(mastodonStatus));
    }

    @Transactional
    public void generateEmbeddings() {

        String model = "gemini-embedding-001";

        final long countLast10Minutes = GeminiRequestEntity.countLast10Minutes(model);

        if(countLast10Minutes < 3) {
            final List<Embedding> nextEmbedding = Embedding.findNextEmbeddings();

            final Map<String, ContentEmbedding> results = generateEmbeddingTextInput.getEmbedding(model, nextEmbedding.stream().collect(Collectors.toMap(Embedding::getUuid, Embedding::getText)));

            nextEmbedding.forEach(embedding -> {
                final ContentEmbedding result = results.get(embedding.getUuid());

                if(result !=null && result.values().isPresent()) {
                    embedding.setEmbedding(Arrays.stream(result.values().get().<Float>toArray(new Float[0])).mapToDouble(Float::doubleValue).toArray());
                }else{
                    LOG.infof("Embedding (%s) has no result", embedding.getUuid());
                }

            });
        }
    }

    @Transactional
    public List<Embedding> getNextEmbeddings(){
        return Embedding.findNextLocalEmbeddings();
    }


    public void generateLocalEmbeddings(){
        final List<Embedding> nextEmbeddings = getNextEmbeddings();


        for (Embedding embedding : nextEmbeddings) {

            final List<String> splitText = splitByLength(embedding.getText(), 500);

            List<double[]> responses = new ArrayList<>();

            for (String text : splitText) {
                EmbeddingRequest request = new EmbeddingRequest("granite-embedding:278m", List.of(text), false);
                final EmbeddingResponse response = ollamaRestClient.generateEmbeddings(request);

                responses.add(response.embeddings().getFirst().stream().mapToDouble(Double::doubleValue).toArray());
            }

            final double[] profileVector = createProfileVector(responses);

            saveEmbedding(embedding.id, profileVector);

            LOG.infof("Save local vector for embedding with uuid: %s", embedding.uuid);


        }


    }

    @Transactional
    public void saveEmbedding(final Long embeddingId, final double[] profileVector) {
        final Embedding embedding = Embedding.findById(embeddingId);

        embedding.setLocalEmbedding(profileVector);
    }


    /**
     * Erstellt einen Profil-Vektor, indem der Durchschnitt mehrerer Vektoren gebildet wird.
     * @param vectors Eine Liste von Vektoren.
     * @return Der durchschnittliche, normalisierte Vektor.
     */
    public static double[] createProfileVector(List<double[]> vectors) {
        if (vectors == null || vectors.isEmpty() || vectors.stream().map(doubles -> doubles.length).distinct().count() > 1) {

            return new double[768];
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

    public static List<String> splitByLength(String str, int laenge) {
        List<String> teile = new ArrayList<>();
        int laengeStr = str.length();
        for (int i = 0; i < laengeStr; i += (laenge - 20)) {
            // Stellt sicher, dass der letzte Teil nicht über das String-Ende hinausgeht
            teile.add(str.substring(i, Math.min(laengeStr, i + laenge)));
        }
        return teile;
    }
}
