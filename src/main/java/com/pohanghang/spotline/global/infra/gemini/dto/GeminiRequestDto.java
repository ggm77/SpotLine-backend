package com.pohanghang.spotline.global.infra.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequestDto(
        List<Content> contents,
        SystemInstruction systemInstruction,
        GenerationConfig generationConfig
) {
    public static GeminiRequestDto of(String text) {
        return new GeminiRequestDto(
                List.of(new Content("user", List.of(new Part(text)))),
                null,
                new GenerationConfig(new ThinkingConfig(0))
        );
    }

    public static GeminiRequestDto ofChat(List<Content> contents, String systemInstruction) {
        return new GeminiRequestDto(
                contents,
                systemInstruction == null || systemInstruction.isBlank()
                        ? null
                        : new SystemInstruction(List.of(new Part(systemInstruction))),
                new GenerationConfig(new ThinkingConfig(0))
        );
    }

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    public record SystemInstruction(List<Part> parts) {}

    public record GenerationConfig(ThinkingConfig thinkingConfig) {}

    public record ThinkingConfig(int thinkingBudget) {}
}
