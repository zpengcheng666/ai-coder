package org.example.ai.mcp;


import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Value("${bigmodel.api-key}")
    private String apiKey;

    @Bean
    public McpToolProvider mcpToolProvider() {
        // 和 MCP 服务通讯 - 注意：这是一个可选的工具提供者
        // 如果连接失败，不会影响主要的 AI 功能
        try {
            McpTransport transport = new HttpMcpTransport.Builder()
                    .sseUrl("https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + apiKey)
                    .logRequests(true) // 开启日志，查看更多信息
                    .logResponses(true)
                    .build();
            // 创建 MCP 客户端
            McpClient mcpClient = new DefaultMcpClient.Builder()
                    .key("yupiMcpClient")
                    .transport(transport)
                    .build();
            
            // 从 MCP 客户端获取工具
            McpToolProvider toolProvider = McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .build();
            return toolProvider;
            
        } catch (Exception e) {
            // 如果 MCP 连接失败，记录警告但不抛出异常
            // 这样可以让应用程序继续运行，只是没有额外的工具功能
            System.err.println("警告：无法连接到 BigModel MCP 服务，将跳过相关工具功能。错误信息: " + e.getMessage());
            
            // 返回一个空的工具提供者
            return McpToolProvider.builder().build();
        }
    }
}