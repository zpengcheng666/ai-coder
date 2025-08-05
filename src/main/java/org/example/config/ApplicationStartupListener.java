package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.ai.rag.EnhancedRagConfig;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

/**
 * 应用启动监听器
 * 在应用完全启动后执行初始化任务
 */
@Component
@Slf4j
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private EnhancedRagConfig enhancedRagConfig;
    
    @Resource
    private EmbeddingStoreIngestor embeddingStoreIngestor;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("应用启动完成，开始初始化RAG文档");
        try {
            enhancedRagConfig.initializeDocuments(embeddingStoreIngestor);
            log.info("RAG文档初始化完成");
        } catch (Exception e) {
            log.error("RAG文档初始化失败", e);
        }
    }
}