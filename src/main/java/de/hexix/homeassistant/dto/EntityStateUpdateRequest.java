package de.hexix.homeassistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record EntityStateUpdateRequest(
    @JsonProperty("state") String state,
    @JsonProperty("attributes") Map<String, Object> attributes
) {}
