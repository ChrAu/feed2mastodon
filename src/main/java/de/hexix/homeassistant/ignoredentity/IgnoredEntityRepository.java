package de.hexix.homeassistant.ignoredentity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class IgnoredEntityRepository implements PanacheRepository<IgnoredEntity> {

    public Set<String> findAllEntityIds() {
        return findAll().stream()
                .map(IgnoredEntity::getEntityId)
                .collect(Collectors.toSet());
    }

    public boolean isEntityIgnored(String entityId) {
        return find("entityId", entityId).count() > 0;
    }
}
