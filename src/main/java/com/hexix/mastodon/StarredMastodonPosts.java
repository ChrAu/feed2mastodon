package com.hexix.mastodon;

import com.google.genai.types.ContentEmbedding;
import com.hexix.ai.GeminiRequestEntity;
import com.hexix.ai.GenerateEmbeddingTextInput;
import com.hexix.mastodon.api.MastodonDtos;
import com.hexix.mastodon.resource.FavouritesService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestScoped
public class StarredMastodonPosts {

    @Inject
    FavouritesService favouritesService;

    @Inject
    GenerateEmbeddingTextInput generateEmbeddingTextInput;


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

                embedding.setEmbedding(Arrays.stream(result.values().get().<Float>toArray(new Float[0])).mapToDouble(Float::doubleValue).toArray());

            });
        }
    }
}
