package org.example.ai;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.example.ai.memory.PersistentChatMemoryProvider;
import org.example.ai.service.ConversationStorageService;
import org.example.ai.tool.InterviewQuestionTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class AiCodeHelperServiceFactory {

    @Resource
    private ChatModel myQwenChatModel;

    @Resource
    @Lazy
    private ContentRetriever enhancedContentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private ConversationStorageService conversationStorageService;

    @Value("${chat.memory.max-messages:20}")
    private int maxMessages;

    /**
     * 持久化聊天内存提供者
     */
    @Bean
    public PersistentChatMemoryProvider persistentChatMemoryProvider() {
        return new PersistentChatMemoryProvider(conversationStorageService, maxMessages);
    }

    /**
     * AI代码助手服务
     */
    @Bean
    public AiCodeHelperService aiCodeHelperService() {
        // 使用持久化会话记忆提供者
        PersistentChatMemoryProvider memoryProvider = persistentChatMemoryProvider();
        
        // 构造ai services
        AiCodeHelperService aiCodeHelperService = AiServices.builder(AiCodeHelperService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(streamingChatModel) // 流式模型输出
                .chatMemoryProvider(memoryProvider) // 持久化会话记忆
                .contentRetriever(enhancedContentRetriever) // 增强RAG内容检索
                .tools(new InterviewQuestionTool()) // 自定义工具调用
                .tools(mcpToolProvider) // MCP工具调用
                .build();

        return aiCodeHelperService;
    }
}
