package de.hexix.ai.dto;

import de.hexix.ai.ThemenEntity;

import java.time.LocalDate;
import java.util.UUID;

public record ThemenDTO(UUID uuid, String thema, LocalDate lastPost) {
    public static ThemenDTO export(ThemenEntity themenEntity) {
        return new ThemenDTO(themenEntity.getUuid(), themenEntity.getThema(), themenEntity.getLastPost());
    }
}
