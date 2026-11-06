package com.civicconnect.controller.api;

import com.civicconnect.dto.ChatRequest;
import com.civicconnect.dto.ChatResponse;
import com.civicconnect.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;

    public ChatApiController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Please ask a question.", null, "en", false));
        }
        ChatResponse response = chatService.processChat(request);
        return ResponseEntity.ok(response);
    }
}
