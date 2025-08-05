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
 * 会话实体 - 用于MySQL存储会话元数据
 */
@Entity
@Table(name = "conversation_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 会话ID
     */
    @Column(name = "conversation_id", unique = true, nullable = false, length = 100)
    private String conversationId;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    /**
     * 会话标题
     */
    @Column(name = "title", length = 200)
    private String title;
    
    /**
     * 会话状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status;
    
    /**
     * 消息数量
     */
    @Column(name = "message_count")
    private Integer messageCount;
    
    /**
     * 总令牌消耗
     */
    @Column(name = "total_tokens")
    private Integer totalTokens;
    
    /**
     * 最后活跃时间
     */
    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;
    
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
    
    public enum SessionStatus {
        ACTIVE, ARCHIVED, DELETED
    }
}