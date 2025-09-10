package com.hexix.homeassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enthält Kontextinformationen zu einem Event oder einer Statusänderung.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("user_id")
    private String userId;

    // Standard-Getter und -Setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
