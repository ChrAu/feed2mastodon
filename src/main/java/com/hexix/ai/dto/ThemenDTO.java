package com.hexix.ai.dto;

import com.hexix.ai.ThemenEntity;

import java.time.LocalDate;

public record ThemenDTO(String uuid, String thema, LocalDate lastPost) {
    public static ThemenDTO export(ThemenEntity themenEntity) {
        return new ThemenDTO(themenEntity.getUuid(), themenEntity.getThema(), themenEntity.getLastPost());
    }
}
