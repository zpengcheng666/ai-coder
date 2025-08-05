package org.example.ai.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话消息模型 - 用于Redis存储
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 会话ID
     */
    private String conversationId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 消息类型：USER, ASSISTANT
     */
    private MessageType messageType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 令牌消耗
     */
    private Integer tokenUsed;
    
    /**
     * 是否是流式响应
     */
    private Boolean isStreaming;
    
    public enum MessageType {
        USER, ASSISTANT
    }
}