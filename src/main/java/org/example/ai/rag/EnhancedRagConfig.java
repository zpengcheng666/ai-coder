package org.example.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
// import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 增强的RAG配置
 * 支持Redis持久化向量存储和动态文档加载
 */
@Configuration
@Slf4j
public class EnhancedRagConfig {

    @Resource
    private EmbeddingModel embeddingModel;

    // @Resource
    // private RedisConnectionFactory redisConnectionFactory;

    @Value("${rag.documents.path:src/main/resources/docs}")
    private String documentsPath;

    @Value("${rag.vector.store.type:redis}")
    private String vectorStoreType;

    @Value("${rag.segment.max-size:800}")
    private int maxSegmentSize;

    @Value("${rag.segment.max-overlap:200}")
    private int maxOverlap;

    @Value("${rag.retrieval.max-results:10}")
    private int maxResults;

    @Value("${rag.retrieval.min-score:0.7}")
    private double minScore;

    /**
     * 向量存储配置
     */
    @Bean
    @Primary
    public EmbeddingStore<TextSegment> enhancedEmbeddingStore() {
        // 暂时使用内存向量存储，Redis向量存储需要额外配置
        log.info("使用内存向量存储");
        return new InMemoryEmbeddingStore<>();
        
        // TODO: Redis向量存储实现，需要Redis Stack或RediSearch模块
        // if ("redis".equalsIgnoreCase(vectorStoreType)) {
        //     log.info("使用Redis向量存储");
        //     return RedisEmbeddingStore.builder()
        //             .connectionFactory(redisConnectionFactory)
        //             .dimension(1536)
        //             .keyPrefix("vector:")
        //             .build();
        // }
    }

    /**
     * 文档解析器
     */
    @Bean
    public DocumentParser documentParser() {
        return new TextDocumentParser();
    }

    /**
     * 文档分割器
     */
    @Bean
    public DocumentSplitter documentSplitter() {
        return new DocumentByParagraphSplitter(maxSegmentSize, maxOverlap);
    }

    /**
     * 文档转换器 - 增加文档元信息
     */
    @Bean
    public TextSegmentTransformer textSegmentTransformer() {
        return textSegment -> {
            String fileName = textSegment.metadata().getString("file_name");
            String fileType = getFileExtension(fileName);
            String enhancedContent = String.format("文档：%s\n类型：%s\n内容：%s", 
                    fileName, fileType, textSegment.text());
            return TextSegment.from(enhancedContent, textSegment.metadata());
        };
    }

    /**
     * 文档摄取器
     */
    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(
            EmbeddingStore<TextSegment> embeddingStore,
            DocumentSplitter documentSplitter,
            TextSegmentTransformer textSegmentTransformer) {
        
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .textSegmentTransformer(textSegmentTransformer)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    /**
     * 内容检索器
     */
    @Bean
    @Lazy
    public ContentRetriever enhancedContentRetriever(EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    /**
     * 初始化时加载文档
     * 移除@PostConstruct，改为手动初始化以避免循环依赖
     */
    public void initializeDocuments(EmbeddingStoreIngestor embeddingStoreIngestor) {
        loadDocumentsAsync(embeddingStoreIngestor);
    }

    /**
     * 异步加载文档到向量存储
     */
    @Async
    public void loadDocumentsAsync(EmbeddingStoreIngestor embeddingStoreIngestor) {
        try {
            log.info("开始加载RAG文档，路径：{}", documentsPath);
            
            Path docsPath = Paths.get(documentsPath);
            if (!docsPath.toFile().exists()) {
                log.warn("文档路径不存在：{}", documentsPath);
                return;
            }

            // 加载所有文档
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(docsPath);
            log.info("找到 {} 个文档", documents.size());

            if (!documents.isEmpty() && embeddingStoreIngestor != null) {
                // 批量摄取文档
                embeddingStoreIngestor.ingest(documents);
                log.info("成功加载 {} 个文档到向量存储", documents.size());
            }
            
        } catch (Exception e) {
            log.error("加载RAG文档失败", e);
        }
    }

    /**
     * 动态添加文档到RAG知识库
     */
    public void addDocument(String filePath, EmbeddingStoreIngestor embeddingStoreIngestor) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("文件不存在：{}", filePath);
                return;
            }

            Document document = FileSystemDocumentLoader.loadDocument(file.toPath());
            if (embeddingStoreIngestor != null) {
                embeddingStoreIngestor.ingest(document);
                log.info("成功添加文档到RAG知识库：{}", filePath);
            }
            
        } catch (Exception e) {
            log.error("添加文档失败：{}", filePath, e);
        }
    }

    /**
     * 重新加载所有文档
     */
    public void reloadAllDocuments(EmbeddingStoreIngestor embeddingStoreIngestor) {
        log.info("重新加载所有RAG文档");
        loadDocumentsAsync(embeddingStoreIngestor);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}