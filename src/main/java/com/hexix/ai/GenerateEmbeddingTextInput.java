package com.hexix.ai;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import com.hexix.mastodon.TextEntity;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@ApplicationScoped
public class GenerateEmbeddingTextInput {

    final Logger LOG = Logger.getLogger(this.getClass());


    @ConfigProperty(name = "gemini.access.token")
    String accessToken;


    @Transactional
    public Map<UUID, ContentEmbedding> getEmbedding(String geminiModel, Map<UUID, String> keyContentList){
        Map<UUID, ContentEmbedding> results = new HashMap<>();

        try(Client client = Client.builder().apiKey(accessToken).build()) {

            List<UUID> keys = new ArrayList<>(keyContentList.size());
            List<String> contentEmbeddings = new ArrayList<>(keyContentList.size());

            for (UUID key : keyContentList.keySet()) {
                if (keyContentList.get(key) != null && !keyContentList.get(key).trim().isEmpty()) {
                    keys.add(key);
                    contentEmbeddings.add(keyContentList.get(key));
                }
            }

            if(contentEmbeddings.isEmpty()){
                return results;
            }

            final GeminiRequestEntity geminiRequestEntity = new GeminiRequestEntity();
            geminiRequestEntity.setModel(geminiModel);
            geminiRequestEntity.setText(contentEmbeddings.stream().map(String::valueOf).collect(Collectors.joining("\n\n")));
            geminiRequestEntity.persist();

            final EmbedContentResponse embedContentResponse = client.models.embedContent(geminiModel, contentEmbeddings, EmbedContentConfig.builder().outputDimensionality(3072).build());

            geminiRequestEntity.setResponse(embedContentResponse.toString());
            final String test = contentEmbeddings.stream().map(s -> "[[" + s +"]]" ).collect(Collectors.joining("\n\n"));

            LOG.debugf("Embeddings UUIDs: %s", keys.stream().map(String::valueOf).collect(Collectors.joining("\n")));
            LOG.debugf("ContentEmbeddings: %s", test) ;
            LOG.debugf("EmbeddingResponse: %s",embedContentResponse);
            final List<ContentEmbedding> geminiContentEmbedding = embedContentResponse.embeddings().get();


            for (int i = 0;  i < geminiContentEmbedding.size(); i++) {
                final ContentEmbedding contentEmbedding = geminiContentEmbedding.get(i);
                results.put(keys.get(i), contentEmbedding);
            }



        } catch (Exception e) {
            Log.warn("Fehler beim generieren von Gemini Embedding Vektoren", e);
        }
        return results;
    }
}
