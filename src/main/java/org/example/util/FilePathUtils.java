package org.example.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件路径工具类，用于处理包含特殊字符的文件路径
 */
public class FilePathUtils {
    
    /**
     * 安全地将文件路径转换为URI
     * @param filePath 文件路径
     * @return URI对象
     * @throws URISyntaxException URI语法异常
     */
    public static URI toURI(String filePath) throws URISyntaxException {
        // 方法1: 使用File类处理路径
        File file = new File(filePath);
        return file.toURI();
    }
    
    /**
     * 使用URL编码处理文件路径并转换为URI
     * @param filePath 文件路径
     * @return URI对象
     * @throws URISyntaxException URI语法异常
     */
    public static URI toURIEncoded(String filePath) throws URISyntaxException {
        // 将路径中的特殊字符进行URL编码
        String encodedPath = filePath.replace("\\", "/");
        if (!encodedPath.startsWith("/")) {
            encodedPath = "/" + encodedPath;
        }
        
        // 对路径中的每个部分进行编码
        String[] parts = encodedPath.split("/");
        StringBuilder encodedPathBuilder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                encodedPathBuilder.append("/");
            }
            if (!parts[i].isEmpty()) {
                encodedPathBuilder.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8));
            }
        }
        
        return new URI("file", "", encodedPathBuilder.toString(), null);
    }
    
    /**
     * 使用Paths.get处理文件路径
     * @param filePath 文件路径
     * @return URI对象
     */
    public static URI toPathURI(String filePath) {
        Path path = Paths.get(filePath);
        return path.toUri();
    }
}