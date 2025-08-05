package org.example.ai.repository;

import org.example.ai.entity.ConversationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话历史Repository
 */
@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {
    
    /**
     * 根据会话ID查询消息历史
     */
    List<ConversationHistory> findByConversationIdOrderByCreateTimeAsc(String conversationId);
    
    /**
     * 根据用户ID查询消息历史（分页）
     */
    Page<ConversationHistory> findByUserIdOrderByCreateTimeDesc(String userId, Pageable pageable);
    
    /**
     * 根据用户ID和时间范围查询消息历史
     */
    @Query("SELECT ch FROM ConversationHistory ch WHERE ch.userId = :userId " +
           "AND ch.createTime BETWEEN :startTime AND :endTime ORDER BY ch.createTime DESC")
    List<ConversationHistory> findByUserIdAndCreateTimeBetween(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户总消息数
     */
    long countByUserId(String userId);
    
    /**
     * 统计用户总token消耗
     */
    @Query("SELECT SUM(ch.tokenUsed) FROM ConversationHistory ch WHERE ch.userId = :userId")
    Long sumTokenUsedByUserId(@Param("userId") String userId);
    
    /**
     * 删除指定时间之前的历史记录
     */
    void deleteByCreateTimeBefore(LocalDateTime time);
}