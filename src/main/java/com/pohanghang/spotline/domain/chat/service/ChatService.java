package com.pohanghang.spotline.domain.chat.service;

import com.pohanghang.spotline.domain.chat.dto.ChatMessageDto;
import com.pohanghang.spotline.domain.chat.dto.ChatRequestDto;
import com.pohanghang.spotline.domain.chat.dto.ChatResponseDto;
import com.pohanghang.spotline.domain.store.entity.Store;
import com.pohanghang.spotline.domain.store.service.StoreService;
import com.pohanghang.spotline.global.exception.CustomException;
import com.pohanghang.spotline.global.exception.constants.ExceptionCode;
import com.pohanghang.spotline.global.infra.gemini.GeminiClient;
import com.pohanghang.spotline.global.infra.gemini.dto.GeminiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT =
            "당신은 매장 사장님을 돕는 SpotLine의 AI 비서입니다.\n"
            + "사장님의 질문에 친절하고 간결하게, 실질적으로 도움이 되는 답변을 한국어로 제공합니다.\n";

    private final GeminiClient geminiClient;
    private final StoreService storeService;

    public ChatResponseDto chat(final ChatRequestDto request) {
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        final List<GeminiRequestDto.Content> contents = new ArrayList<>();
        for (ChatMessageDto message : request.messages()) {
            if (message == null || message.content() == null || message.content().isBlank()) {
                throw new CustomException(ExceptionCode.INVALID_REQUEST);
            }
            contents.add(new GeminiRequestDto.Content(
                    toGeminiRole(message.role()),
                    List.of(new GeminiRequestDto.Part(message.content()))
            ));
        }

        // 마지막 메시지가 사용자 발화여야 모델 응답을 받을 수 있다.
        if (!"user".equals(contents.get(contents.size() - 1).role())) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }

        return new ChatResponseDto(geminiClient.chat(contents, buildSystemInstruction()));
    }

    private String toGeminiRole(final String role) {
        if (role == null) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }
        return switch (role.toLowerCase()) {
            case "user" -> "user";
            case "model", "assistant", "bot" -> "model";
            default -> throw new CustomException(ExceptionCode.INVALID_REQUEST);
        };
    }

    private String buildSystemInstruction() {
        Store store = storeService.getDefaultStore();
        if (store == null) {
            return SYSTEM_PROMPT;
        }
        return SYSTEM_PROMPT + String.format("가게명: %s, 업종: %s\n",
                store.getStoreName(), store.getBusinessType());
    }
}
