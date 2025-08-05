package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.ai.AiCodeHelperService;

import org.example.ai.entity.ConversationSession;
import org.example.ai.rag.EnhancedRagConfig;
import org.example.ai.service.ConversationStorageService;


import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Resource
    private ConversationStorageService conversationStorageService;

    @Resource
    private EnhancedRagConfig enhancedRagConfig;

    @Resource
    private dev.langchain4j.store.embedding.EmbeddingStoreIngestor embeddingStoreIngestor;

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("service", "ai-code-helper");
        return ResponseEntity.ok(result);
    }

    /**
     * 流式聊天接口
     */
    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(
            @RequestParam(required = false) String memoryId,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false, defaultValue = "default_user") String userId,
            @RequestParam String message,
            HttpServletRequest request) {
        
        // 兼容前端传入的memoryId参数
        String actualConversationId = memoryId != null ? memoryId : 
                                     conversationId != null ? conversationId : 
                                     "conversation_" + System.currentTimeMillis();
        
        // 保存用户消息
        String userMessageId = conversationStorageService.saveUserMessage(actualConversationId, userId, message);
        log.info("用户消息已保存: {}", userMessageId);
        
        // 返回AI响应流
        return aiCodeHelperService.chatStream(actualConversationId, message)
                .doOnNext(chunk -> log.debug("AI响应片段: {}", chunk))
                .doOnComplete(() -> {
                    // 流式响应完成后，可以在这里处理完整响应的保存
                    log.info("会话 {} 的流式响应已完成", actualConversationId);
                })
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * 普通聊天接口（非流式）
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chatSync(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 保存用户消息
            String userMessageId = conversationStorageService.saveUserMessage(
                    request.getConversationId(), request.getUserId(), request.getMessage());
            
            // 获取AI响应
            String response = aiCodeHelperService.chat(request.getConversationId(), request.getMessage());
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 保存AI消息（估算token使用量）
            String aiMessageId = conversationStorageService.saveAiMessage(
                    request.getConversationId(), 
                    request.getUserId(), 
                    response, 
                    response.length() / 4, // 简单估算token数量
                    false
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("userMessageId", userMessageId);
            result.put("aiMessageId", aiMessageId);
            result.put("response", response);
            result.put("responseTime", responseTime);
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("聊天处理失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "聊天处理失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/conversation")
    public ResponseEntity<Map<String, Object>> createConversation(
            @RequestBody CreateConversationRequest request) {
        
        try {
            String conversationId = conversationStorageService.createConversation(
                    request.getUserId(), request.getTitle());
            
            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("title", request.getTitle());
            result.put("createTime", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("创建会话失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "创建会话失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取用户会话列表
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getUserConversations(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<ConversationSession> conversations = conversationStorageService.getUserConversations(userId, page, size);
            
            Map<String, Object> result = new HashMap<>();
            result.put("conversations", conversations);
            result.put("page", page);
            result.put("size", size);
            result.put("total", conversations.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取会话列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 添加文档到RAG知识库
     */
    @PostMapping("/rag/document")
    public ResponseEntity<Map<String, Object>> addDocument(@RequestBody AddDocumentRequest request) {
        try {
            enhancedRagConfig.addDocument(request.getFilePath(), embeddingStoreIngestor);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "文档添加成功");
            result.put("filePath", request.getFilePath());
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("添加文档失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "添加文档失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 重新加载RAG文档
     */
    @PostMapping("/rag/reload")
    public ResponseEntity<Map<String, Object>> reloadDocuments() {
        try {
            enhancedRagConfig.reloadAllDocuments(embeddingStoreIngestor);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "文档重新加载成功");
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("重新加载文档失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "重新加载文档失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 请求DTO类
    public static class ChatRequest {
        private String conversationId;
        private String userId;
        private String message;

        // Getters and Setters
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class CreateConversationRequest {
        private String userId;
        private String title;

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    public static class AddDocumentRequest {
        private String filePath;

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }
}