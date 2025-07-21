package com.hexix.ai.dto;

import java.util.List;

public record EmbeddingRequest(String model, List<String> input, Boolean truncate) {}
