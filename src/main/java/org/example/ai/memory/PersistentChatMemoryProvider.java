package org.example.ai.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import lombok.RequiredArgsConstructor;
import org.example.ai.service.ConversationStorageService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 持久化聊天内存提供者
 * 为每个会话提供独立的持久化内存
 */
@RequiredArgsConstructor
public class PersistentChatMemoryProvider implements ChatMemoryProvider {
    
    private final ConversationStorageService storageService;
    private final int maxMessages;
    private final ConcurrentHashMap<Object, PersistentChatMemory> memories = new ConcurrentHashMap<>();
    
    @Override
    public ChatMemory get(Object memoryId) {
        return memories.computeIfAbsent(memoryId, id -> 
                new PersistentChatMemory(storageService, String.valueOf(id), maxMessages));
    }
    
    /**
     * 清理指定会话的内存缓存
     */
    public void evict(Object memoryId) {
        memories.remove(memoryId);
    }
    
    /**
     * 清理所有内存缓存
     */
    public void evictAll() {
        memories.clear();
    }
}