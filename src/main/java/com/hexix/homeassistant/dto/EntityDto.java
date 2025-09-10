package com.hexix.homeassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Stellt eine einzelne Entität aus der Home Assistant API dar.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityDto {

    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("state")
    private String state;

    @JsonProperty("attributes")
    private AttributesDto attributes;

    @JsonProperty("last_changed")
    private String lastChanged;

    @JsonProperty("last_reported")
    private String lastReported;

    @JsonProperty("last_updated")
    private String lastUpdated;

    @JsonProperty("context")
    private ContextDto context;

    // Standard-Getter und -Setter

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public AttributesDto getAttributes() {
        return attributes;
    }

    public void setAttributes(AttributesDto attributes) {
        this.attributes = attributes;
    }

    public String getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(String lastChanged) {
        this.lastChanged = lastChanged;
    }

    public String getLastReported() {
        return lastReported;
    }

    public void setLastReported(String lastReported) {
        this.lastReported = lastReported;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public ContextDto getContext() {
        return context;
    }

    public void setContext(ContextDto context) {
        this.context = context;
    }
}
