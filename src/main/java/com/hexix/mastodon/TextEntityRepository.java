package com.hexix.mastodon;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class TextEntityRepository {


    @Inject
    EntityManager em;

    /**
     * Finds an entity by its primary key.
     * @param id The primary key.
     * @return An Optional containing the entity if found, otherwise empty.
     */
    @Transactional
    public Optional<TextEntity> findById(Long id){
        return Optional.ofNullable(em.find(TextEntity.class, id));
    }


    /**
     * Persists a new entity in the database.
     * @param entity The entity to persist.
     */
    @Transactional
    public void persist(TextEntity entity) {
        em.persist(entity);
    }

    /**
     * Merges the state of the given entity into the current persistence context.
     * @param entity The entity to merge.
     * @return The managed instance that the state was merged to.
     */
    @Transactional
    public TextEntity merge(TextEntity entity) {
        return em.merge(entity);
    }

    /**
     * Deletes an entity from the database.
     * @param entity The entity to delete.
     */
    @Transactional
    public void delete(TextEntity entity) {
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

}
