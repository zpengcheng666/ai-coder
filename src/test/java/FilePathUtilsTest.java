import org.example.util.FilePathUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class FilePathUtilsTest {
    public static void main(String[] args) {
        String filePath = "E:\\DownLoad\\图片壁纸\\03.jpg";
        
        try {
            // 原始导致错误的方法
            // URI uri = URI.create(filePath); // 这行会报错
            
            // 正确的处理方式1: 使用File类
            URI uri1 = FilePathUtils.toURI(filePath);
            System.out.println("使用File.toURI(): " + uri1);
            
            // 正确的处理方式2: 使用Paths.get
            URI uri2 = FilePathUtils.toPathURI(filePath);
            System.out.println("使用Paths.get().toUri(): " + uri2);
            
            // 正确的处理方式3: 使用URL编码
            URI uri3 = FilePathUtils.toURIEncoded(filePath);
            System.out.println("使用URL编码: " + uri3);
            
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}