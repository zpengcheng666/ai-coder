import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.example.SpringBootDemoApplication;
import org.example.ai.AiCodeHelper;
import org.example.util.FilePathUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SpringBootDemoApplication.class)
public class AiCodeHelperApplicationTest {

    @Resource
    private AiCodeHelper aiCodeHelper;

    @Test
    void chat() {
        aiCodeHelper.chat("你好我是程序员鱼皮");
    }

    @Test
    void chatWithMessage() {
        try {
            String filePath = "E:\\DownLoad\\图片壁纸\\1.jpg";
            // 使用FilePathUtils工具类安全地转换文件路径为URI
            String fileUri = FilePathUtils.toURI(filePath).toString();
            
            UserMessage userMessage = UserMessage.from(TextContent.from("这个图片是什么"),
                    ImageContent.from(fileUri));

            aiCodeHelper.chatWithMessage(userMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}