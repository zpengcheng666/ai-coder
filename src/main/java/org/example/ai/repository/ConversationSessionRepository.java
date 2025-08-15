package org.example.ai.repository;

import org.example.ai.entity.ConversationSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话Session Repository
 */
@Repository
public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {
    
    /**
     * 根据会话ID查询
     */
    Optional<ConversationSession> findByConversationId(String conversationId);
    
    /**
     * 根据用户ID查询会话列表（分页）
     */
    Page<ConversationSession> findByUserIdAndStatusOrderByLastActiveTimeDesc(
            String userId, ConversationSession.SessionStatus status, Pageable pageable);
    
    /**
     * 根据用户ID查询所有活跃会话
     */
    List<ConversationSession> findByUserIdAndStatusOrderByLastActiveTimeDesc(
            String userId, ConversationSession.SessionStatus status);
    
    /**
     * 更新会话的最后活跃时间和消息数
     */
    @Modifying
    @Query("UPDATE ConversationSession cs SET cs.lastActiveTime = :lastActiveTime, " +
           "cs.messageCount = cs.messageCount + 1, cs.totalTokens = cs.totalTokens + :tokenUsed " +
           "WHERE cs.conversationId = :conversationId")
    void updateSessionActivity(@Param("conversationId") String conversationId,
                              @Param("lastActiveTime") LocalDateTime lastActiveTime,
                              @Param("tokenUsed") Integer tokenUsed);
    
    /**
     * 查询需要归档的会话（超过指定时间未活跃）
     */
    @Query("SELECT cs FROM ConversationSession cs WHERE cs.status = 'ACTIVE' " +
           "AND cs.lastActiveTime < :archiveTime")
    List<ConversationSession> findSessionsToArchive(@Param("archiveTime") LocalDateTime archiveTime);

    /**
     * 软删除会话（将状态置为DELETED）
     */
    @Modifying
    @Query("UPDATE ConversationSession cs SET cs.status = 'DELETED', cs.updateTime = CURRENT_TIMESTAMP " +
           "WHERE cs.conversationId = :conversationId AND cs.userId = :userId")
    int softDeleteByConversationIdAndUserId(@Param("conversationId") String conversationId,
                                            @Param("userId") String userId);
}