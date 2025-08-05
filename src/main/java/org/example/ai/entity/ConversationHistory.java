package org.example.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 会话历史实体 - 用于MySQL持久化存储
 */
@Entity
@Table(name = "conversation_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 消息ID
     */
    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;
    
    /**
     * 会话ID
     */
    @Column(name = "conversation_id", nullable = false, length = 100)
    private String conversationId;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    /**
     * 消息类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;
    
    /**
     * 消息内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    /**
     * 令牌消耗
     */
    @Column(name = "token_used")
    private Integer tokenUsed;
    
    /**
     * 响应时间（毫秒）
     */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    /**
     * 模型名称
     */
    @Column(name = "model_name", length = 100)
    private String modelName;
    
    /**
     * 是否是流式响应
     */
    @Column(name = "is_streaming")
    private Boolean isStreaming;
    
    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    public enum MessageType {
        USER, ASSISTANT
    }
}