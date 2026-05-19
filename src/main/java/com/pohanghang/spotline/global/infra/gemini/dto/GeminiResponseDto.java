package com.pohanghang.spotline.global.infra.gemini.dto;

import java.util.List;

public record GeminiResponseDto(
        List<Candidate> candidates
) {
    public record Candidate(
            Content content
    ) {}

    public record Content(
            List<Part> parts
    ) {}

    public record Part(
            String text
    ) {}

    public String getFirstText() {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        Candidate candidate = candidates.get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return "";
        }
        return candidate.content().parts().get(0).text();
    }
}
