package com.hexix.homeassistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexix.homeassistant.dto.AttributesDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Hilfsklasse für MapStruct, um komplexe Objekte in JSON-Strings
 * zu serialisieren und wieder zu deserialisieren.
 */
@ApplicationScoped
public class AttributeMapperHelper {

    final static Logger LOG = Logger.getLogger(AttributeMapperHelper.class);

    @Inject
    ObjectMapper objectMapper;

    public String attributesToString(AttributesDto attributes) {
        if (attributes == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            // Hier sollte ein geeignetes Logging und Error-Handling stattfinden
            throw new RuntimeException("Fehler bei der Serialisierung der Attribute", e);
        }
    }

    public AttributesDto stringToAttributes(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AttributesDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Fehler bei der Deserialisierung der Attribute", e);
        }
    }
}
