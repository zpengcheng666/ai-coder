package org.example.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.RequiredArgsConstructor;
import org.example.ai.service.ConversationStorageService;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久化聊天内存实现
 * 集成Redis和MySQL存储
 */
@RequiredArgsConstructor
public class PersistentChatMemory implements ChatMemory {
    
    private final ConversationStorageService storageService;
    private final String conversationId;
    private final int maxMessages;
    private final List<ChatMessage> messages = new ArrayList<>();
    private boolean loaded = false;
    
    @Override
    public Object id() {
        return conversationId;
    }
    
    @Override
    public void add(ChatMessage message) {
        ensureLoaded();
        messages.add(message);
        
        // 保持消息数量限制
        if (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }
    
    @Override
    public List<ChatMessage> messages() {
        ensureLoaded();
        return new ArrayList<>(messages);
    }
    
    @Override
    public void clear() {
        messages.clear();
        loaded = true; // 标记为已加载，避免重新从存储加载
    }
    
    /**
     * 确保消息已从存储中加载
     */
    private void ensureLoaded() {
        if (!loaded) {
            List<ChatMessage> storedMessages = storageService.getConversationMessages(conversationId, maxMessages);
            messages.addAll(storedMessages);
            loaded = true;
        }
    }
}