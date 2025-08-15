package org.example.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai.entity.ConversationHistory;
import org.example.ai.entity.ConversationSession;
import org.example.ai.model.ConversationMessage;
import org.example.ai.repository.ConversationHistoryRepository;
import org.example.ai.repository.ConversationSessionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能会话存储服务
 * 管理Redis高频缓存和MySQL持久化存储
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationStorageService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ConversationHistoryRepository historyRepository;
    private final ConversationSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;
    
    // Redis键前缀
    private static final String REDIS_CONVERSATION_PREFIX = "conversation:";
    private static final String REDIS_MESSAGE_PREFIX = "message:";
    
    // 缓存过期时间（小时）
    private static final int REDIS_EXPIRE_HOURS = 24;
    
    /**
     * 保存用户消息
     */
    public String saveUserMessage(String conversationId, String userId, String content) {
        String messageId = UUID.randomUUID().toString();
        ConversationMessage message = ConversationMessage.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .userId(userId)
                .messageType(ConversationMessage.MessageType.USER)
                .content(content)
                .createTime(LocalDateTime.now())
                .isStreaming(false)
                .build();
        
        // 保存到Redis
        saveToRedis(message);
        
        // 异步保存到MySQL
        saveToMySQLAsync(message, null);
        
        // 更新会话信息
        updateSessionActivity(conversationId, userId, 0);
        
        return messageId;
    }
    
    /**
     * 保存AI响应消息
     */
    public String saveAiMessage(String conversationId, String userId, String content, 
                               Integer tokenUsed, boolean isStreaming) {
        String messageId = UUID.randomUUID().toString();
        ConversationMessage message = ConversationMessage.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .userId(userId)
                .messageType(ConversationMessage.MessageType.ASSISTANT)
                .content(content)
                .createTime(LocalDateTime.now())
                .tokenUsed(tokenUsed)
                .isStreaming(isStreaming)
                .build();
        
        // 保存到Redis
        saveToRedis(message);
        
        // 异步保存到MySQL
        saveToMySQLAsync(message, null);
        
        // 更新会话信息
        updateSessionActivity(conversationId, userId, tokenUsed);
        
        return messageId;
    }
    
    /**
     * 从Redis获取会话消息（用于LangChain4j内存）
     */
    public List<ChatMessage> getConversationMessages(String conversationId, int maxMessages) {
        String redisKey = REDIS_CONVERSATION_PREFIX + conversationId;
        List<String> messageIds = redisTemplate.opsForList().range(redisKey, -maxMessages, -1);
        
        if (messageIds == null || messageIds.isEmpty()) {
            // Redis中没有，从MySQL加载
            return loadFromMySQL(conversationId, maxMessages);
        }
        
        return messageIds.stream()
                .map(this::getMessageFromRedis)
                .filter(msg -> msg != null)
                .map(this::convertToChatMessage)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建新会话
     */
    public String createConversation(String userId, String title) {
        String conversationId = UUID.randomUUID().toString();
        
        ConversationSession session = ConversationSession.builder()
                .conversationId(conversationId)
                .userId(userId)
                .title(title != null ? title : "新对话")
                .status(ConversationSession.SessionStatus.ACTIVE)
                .messageCount(0)
                .totalTokens(0)
                .lastActiveTime(LocalDateTime.now())
                .build();
        
        sessionRepository.save(session);
        
        return conversationId;
    }
    
    /**
     * 获取用户会话列表
     */
    public List<ConversationSession> getUserConversations(String userId, int page, int size) {
        return sessionRepository.findByUserIdAndStatusOrderByLastActiveTimeDesc(
                userId, ConversationSession.SessionStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(page, size)).getContent();
    }

    /**
     * 软删除会话
     */
    @Transactional
    public boolean deleteConversation(String conversationId, String userId) {
        try {
            int updated = sessionRepository.softDeleteByConversationIdAndUserId(conversationId, userId);
            if (updated > 0) {
                // 同步清理Redis中的会话列表（非强制）
                String conversationKey = REDIS_CONVERSATION_PREFIX + conversationId;
                redisTemplate.delete(conversationKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("软删除会话失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 保存到Redis
     */
    private void saveToRedis(ConversationMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            String messageKey = REDIS_MESSAGE_PREFIX + message.getMessageId();
            String conversationKey = REDIS_CONVERSATION_PREFIX + message.getConversationId();
            
            // 保存消息内容
            redisTemplate.opsForValue().set(messageKey, messageJson, REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
            
            // 将消息ID添加到会话列表
            redisTemplate.opsForList().rightPush(conversationKey, message.getMessageId());
            redisTemplate.expire(conversationKey, REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
            
            // 限制会话消息数量（保持最近50条）
            redisTemplate.opsForList().trim(conversationKey, -50, -1);
            
        } catch (JsonProcessingException e) {
            log.error("保存消息到Redis失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从Redis获取消息
     */
    private ConversationMessage getMessageFromRedis(String messageId) {
        try {
            String messageKey = REDIS_MESSAGE_PREFIX + messageId;
            String messageJson = redisTemplate.opsForValue().get(messageKey);
            if (messageJson != null) {
                return objectMapper.readValue(messageJson, ConversationMessage.class);
            }
        } catch (JsonProcessingException e) {
            log.error("从Redis读取消息失败: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * 异步保存到MySQL
     */
    @Async
    public void saveToMySQLAsync(ConversationMessage message, Long responseTimeMs) {
        try {
            ConversationHistory history = ConversationHistory.builder()
                    .messageId(message.getMessageId())
                    .conversationId(message.getConversationId())
                    .userId(message.getUserId())
                    .messageType(ConversationHistory.MessageType.valueOf(message.getMessageType().name()))
                    .content(message.getContent())
                    .tokenUsed(message.getTokenUsed())
                    .responseTimeMs(responseTimeMs)
                    .modelName("qwen-max") // 可以从配置中获取
                    .isStreaming(message.getIsStreaming())
                    .build();
            
            historyRepository.save(history);
        } catch (Exception e) {
            log.error("保存消息到MySQL失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从MySQL加载消息
     */
    private List<ChatMessage> loadFromMySQL(String conversationId, int maxMessages) {
        List<ConversationHistory> histories = historyRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
        
        return histories.stream()
                .skip(Math.max(0, histories.size() - maxMessages))
                .map(this::convertToChatMessage)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为LangChain4j的ChatMessage
     */
    private ChatMessage convertToChatMessage(ConversationMessage message) {
        if (message.getMessageType() == ConversationMessage.MessageType.USER) {
            return UserMessage.from(message.getContent());
        } else {
            return AiMessage.from(message.getContent());
        }
    }
    
    private ChatMessage convertToChatMessage(ConversationHistory history) {
        if (history.getMessageType() == ConversationHistory.MessageType.USER) {
            return UserMessage.from(history.getContent());
        } else {
            return AiMessage.from(history.getContent());
        }
    }
    
    /**
     * 更新会话活跃度
     */
    @Transactional
    public void updateSessionActivity(String conversationId, String userId, Integer tokenUsed) {
        try {
            sessionRepository.updateSessionActivity(conversationId, LocalDateTime.now(), tokenUsed != null ? tokenUsed : 0);
        } catch (Exception e) {
            log.error("更新会话活跃度失败: {}", e.getMessage(), e);
        }
    }
}