package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.BO.SessionCreateBO;
import com.kakarote.ai_crm.entity.VO.ChatMessageVO;
import com.kakarote.ai_crm.entity.VO.ChatSessionVO;
import com.kakarote.ai_crm.service.IChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI聊天控制器
 */
@RestController
@RequestMapping("/chat")
@Tag(name = "AI聊天")
public class ChatController {

    @Autowired
    private IChatService chatService;

    @PostMapping("/session/create")
    @Operation(summary = "创建会话")
    public Result<Long> createSession(@Valid @RequestBody SessionCreateBO sessionCreateBO) {
        Long sessionId = chatService.createSession(sessionCreateBO);
        return Result.ok(sessionId);
    }

    @GetMapping("/session/list")
    @Operation(summary = "获取会话列表")
    public Result<List<ChatSessionVO>> getSessionList() {
        return Result.ok(chatService.getSessionList());
    }

    @PostMapping("/session/delete/{id}")
    @Operation(summary = "删除会话")
    public Result<String> deleteSession(@PathVariable("id") Long id) {
        chatService.deleteSession(id);
        return Result.ok();
    }

    @GetMapping("/message/list/{sessionId}")
    @Operation(summary = "获取会话消息历史")
    public Result<List<ChatMessageVO>> getMessageList(@PathVariable("sessionId") Long sessionId) {
        return Result.ok(chatService.getMessageList(sessionId));
    }

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（流式响应）")
    public Flux<ServerSentEvent<String>> send(@RequestBody ChatSendBO sendBO) {
        return chatService.streamChat(sendBO.getSessionId(), sendBO.getContent())
            .filter(chunk -> chunk != null && !chunk.isEmpty())
            .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk)
                .build());
    }

    @PostMapping("/sendSync")
    @Operation(summary = "发送消息（同步响应）")
    public Result<String> sendSync(@RequestBody ChatSendBO sendBO) {
        String response = chatService.chat(sendBO.getSessionId(), sendBO.getContent());
        return Result.ok(response);
    }
}
