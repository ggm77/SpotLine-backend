package com.pohanghang.spotline.domain.chat.dto;

import java.util.List;

/**
 * 무상태 멀티턴 챗봇 요청. 클라이언트가 이전 대화 전체를 messages 로 함께 보낸다.
 * 마지막 메시지는 사용자(user) 발화여야 한다.
 */
public record ChatRequestDto(
        List<ChatMessageDto> messages
) {
}
