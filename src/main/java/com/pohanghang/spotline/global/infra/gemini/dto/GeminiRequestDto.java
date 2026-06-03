package com.pohanghang.spotline.global.infra.gemini.dto;

import java.util.List;

public record GeminiRequestDto(
        List<Content> contents,
        GenerationConfig generationConfig
) {
    public static GeminiRequestDto of(String text) {
        return new GeminiRequestDto(
                List.of(new Content("user", List.of(new Part(text)))),
                new GenerationConfig(new ThinkingConfig(0))
        );
    }

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(ThinkingConfig thinkingConfig) {}

    public record ThinkingConfig(int thinkingBudget) {}
}
