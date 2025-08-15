package com.hexix.ai.bot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the structured JSON response from the AI.
 * Using a record for immutability and conciseness.
 */
public record VikiResponse(String content, List<String> hashTags) {

    @JsonCreator
    public VikiResponse(@JsonProperty("content") String content,
                        @JsonProperty("hashTags") List<String> hashTags) {
        this.content = content;
        this.hashTags = hashTags;
    }

    @Override
    public String toString() {
        return "VikiResponse{" +
                "content='" + content + '\'' +
                ", hashTags=" + hashTags +
                '}';
    }
}
