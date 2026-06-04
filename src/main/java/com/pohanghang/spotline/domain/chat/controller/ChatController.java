package com.pohanghang.spotline.domain.chat.controller;

import com.pohanghang.spotline.domain.chat.dto.ChatRequestDto;
import com.pohanghang.spotline.domain.chat.dto.ChatResponseDto;
import com.pohanghang.spotline.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chat(@RequestBody final ChatRequestDto request) {
        return ResponseEntity.ok(chatService.chat(request));
    }
}
