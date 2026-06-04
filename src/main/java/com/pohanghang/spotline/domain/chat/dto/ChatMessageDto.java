package com.pohanghang.spotline.domain.chat.dto;

/**
 * 대화 한 턴. role 은 "user" 또는 "model"(="assistant").
 */
public record ChatMessageDto(
        String role,
        String content
) {
}
