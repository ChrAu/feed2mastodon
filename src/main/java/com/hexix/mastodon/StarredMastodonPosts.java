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
import com.hexix.util.VektorUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    @ConfigProperty(name = "local.model", defaultValue = "granite-embedding:278m")
    String localModel;

    @ConfigProperty(name = "gemini.embedding.model")
    String geminiModel;


    @Inject
    @RestClient
    OllamaRestClient ollamaRestClient;


    public void collectNewStarredPosts(){
        final List<MastodonDtos.MastodonStatus> newFavourites = favouritesService.getNewFavourites();

        LOG.infof("Neue Favourites: %s",  newFavourites.size());


        newFavourites.forEach(mastodonStatus -> favouritesService.createEmbedding(mastodonStatus));
    }

    @Transactional
    public void generateEmbeddings() {

        String model = geminiModel;

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
                EmbeddingRequest request = new EmbeddingRequest(localModel, List.of(text), false);
                final EmbeddingResponse response = ollamaRestClient.generateEmbeddings(request);

                responses.add(response.embeddings().getFirst().stream().mapToDouble(Double::doubleValue).toArray());
            }



            final double[] profileVector = VektorUtil.createProfileVector(responses);

            saveEmbedding(embedding.id, profileVector);

            LOG.infof("Save local vector for embedding with uuid: %s", embedding.uuid);


        }


    }

    @Transactional
    public void saveEmbedding(final Long embeddingId, final double[] profileVector) {
        final Embedding embedding = Embedding.findById(embeddingId);

        embedding.setLocalEmbedding(profileVector);
    }




    public static List<String> splitByLength(String str, int laenge) {
        if(str == null){
            return Collections.emptyList();
        }
        List<String> teile = new ArrayList<>();
        int laengeStr = str.length();
        for (int i = 0; i < laengeStr; i += (laenge - 20)) {
            // Stellt sicher, dass der letzte Teil nicht über das String-Ende hinausgeht
            teile.add(str.substring(i, Math.min(laengeStr, i + laenge)));
        }
        return teile;
    }
}
