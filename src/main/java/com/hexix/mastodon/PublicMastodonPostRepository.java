package com.hexix.mastodon;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository-Klasse für den Zugriff auf PublicMastodonPostEntity-Objekte.
 * Ersetzt die statischen Find-Methoden von Panache.
 */
@ApplicationScoped
public class PublicMastodonPostRepository {

    @Inject
    EntityManager em;

    /**
     * Finds the next 10 PublicMastodonPostEntity objects that do not have an embedding vector string.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */
    public List<PublicMastodonPostEntity> findNextPublicMastodonPost() {
        TypedQuery<PublicMastodonPostEntity> query = em.createNamedQuery(PublicMastodonPostEntity.FIND_NEXT_PUBLIC_MASTODON_POST, PublicMastodonPostEntity.class);
        query.setMaxResults(10);
        return query.getResultList();
    }

    /**
     * Finds all PublicMastodonPostEntity objects that have an embedding vector string but no cosine distance.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */
    public List<PublicMastodonPostEntity> findAllComparable() {
        return em.createNamedQuery(PublicMastodonPostEntity.FIND_ALL_COMPARABLE, PublicMastodonPostEntity.class).getResultList();
    }

    /**
     * Finds a PublicMastodonPostEntity object by its mastodonId.
     * @param id The mastodonId of the PublicMastodonPostEntity to find.
     * @return An Optional containing the entity if found, otherwise empty.
     */
    public Optional<PublicMastodonPostEntity> findByMastodonId(final String id) {
        try {
            PublicMastodonPostEntity result = em.createNamedQuery(PublicMastodonPostEntity.FIND_BY_MASTODON_ID, PublicMastodonPostEntity.class)
                    .setParameter(PublicMastodonPostEntity.PARAM_MASTODON_ID, id)
                    .getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Finds all PublicMastodonPostEntity objects that have a negative weight and an embedding vector string.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */
    public List<PublicMastodonPostEntity> findAllNegativPosts() {
        return em.createNamedQuery(PublicMastodonPostEntity.FIND_ALL_NEGATIVE_POSTS, PublicMastodonPostEntity.class).getResultList();
    }


    /**
     * Finds all PublicMastodonPostEntity objects that have a calculated embedding and are older than 2 days.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */
    public List<PublicMastodonPostEntity> findAllCalcedEmbeddings() {
        TypedQuery<PublicMastodonPostEntity> query = em.createNamedQuery(PublicMastodonPostEntity.FIND_ALL_CALCED_EMBEDDINGS, PublicMastodonPostEntity.class);
        query.setParameter("date", LocalDateTime.now().minusDays(2));
        return query.getResultList();
    }


    /**
     * Finds all PublicMastodonPostEntity objects that have no embedding vector string,
     * no post text, and no URL text.
     * @return A list of PublicMastodonPostEntity objects matching the criteria.
     */

    public List<PublicMastodonPostEntity> findAllNoEmbeddingAndText() {
        TypedQuery<PublicMastodonPostEntity> query = em.createNamedQuery(PublicMastodonPostEntity.FIND_ALL_NO_EMBEDDING_AND_TEXT, PublicMastodonPostEntity.class);
        query.setMaxResults(15);
        return query.getResultList();
    }

    /**
     * Finds all PublicMastodonPostEntity objects, sorted by creation date descending.
     * @return A list of all PublicMastodonPostEntity objects.
     */
    public List<PublicMastodonPostEntity> findAll() {
        return em.createNamedQuery(PublicMastodonPostEntity.FIND_ALL, PublicMastodonPostEntity.class).getResultList();
    }

    /**
     * Finds an entity by its primary key.
     * @param id The primary key.
     * @return An Optional containing the entity if found, otherwise empty.
     */
    @Transactional
    public Optional<PublicMastodonPostEntity> findById(Long id){
        return Optional.ofNullable(em.find(PublicMastodonPostEntity.class, id));
    }

    /**
     * Persists a new entity in the database.
     * @param entity The entity to persist.
     */
    @Transactional
    public void persist(PublicMastodonPostEntity entity) {
        em.persist(entity);
    }

    /**
     * Merges the state of the given entity into the current persistence context.
     * @param entity The entity to merge.
     * @return The managed instance that the state was merged to.
     */
    @Transactional
    public PublicMastodonPostEntity merge(PublicMastodonPostEntity entity) {
        return em.merge(entity);
    }

    /**
     * Deletes an entity from the database.
     * @param entity The entity to delete.
     */
    @Transactional
    public void delete(PublicMastodonPostEntity entity) {
        // Stellt sicher, dass die Entity gemanaged ist, bevor sie gelöscht wird.
        if (em.contains(entity)) {
            em.remove(entity);
        } else {
            // Versuche, die Entität anhand ihrer ID zu finden und zu löschen
            if (entity.id != null) {
                findById(entity.id).ifPresent(em::remove);
            }
        }
    }

    public Optional<PublicMastodonPostEntity> findNextVikiComment() {
        return Optional.ofNullable(em.createNamedQuery(PublicMastodonPostEntity.FIND_BY_NO_VIKI_COMMENT, PublicMastodonPostEntity.class).getSingleResultOrNull());
    }
}

