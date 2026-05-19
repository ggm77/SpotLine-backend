package com.pohanghang.spotline.global.infra.gemini.dto;

import java.util.List;

public record GeminiRequestDto(
        List<Content> contents
) {
    public static GeminiRequestDto of(String text) {
        return new GeminiRequestDto(List.of(new Content(List.of(new Part(text)))));
    }

    public record Content(
            List<Part> parts
    ) {}

    public record Part(
            String text
    ) {}
}
