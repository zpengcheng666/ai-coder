package org.example.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;

import java.util.List;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rag 配置
 */
@Configuration
public class RagConfig {

    @Value("${rag.documents.path}")
    private String documentsPath;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

//    @Resource
//    private OpenAiChatModel openAiChatModel;

    @Bean
    public ContentRetriever contentRetriever() {
        //1.rag 加载文档目录中的所有文档
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(documentsPath);

        //2.文档切割，每个文档按照段落来切割，最大1000个字符，每次最大重叠200个字符
        DocumentByParagraphSplitter documentSplitter = new DocumentByParagraphSplitter(1000, 200 );

        //3.自定义文档加载器，把文档转换为向量并保存至向量数据中
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                //为了提高文档质量，为每个文档切割后的碎片添加文档名称作为元信息
                .textSegmentTransformer(textSegment ->
                        TextSegment.from(textSegment.metadata().getString("file_name") + "\n" + textSegment.text()))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        //加载文档
        ingestor.ingest(documents);

        //4.自定义内容加载器
        //最大返回10条
        //最小相似度0.7

        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(10)//最大返回10条
                .minScore(0.7) //最小相似度0.7
                .build();
    }
}
