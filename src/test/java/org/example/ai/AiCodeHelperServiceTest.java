package org.example.ai;

import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeHelperServiceTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Test
    void chat() {
        String result = aiCodeHelperService.chat("1", "你好我是小王");
        System.out.println(result);
    }

    @Test
    void chatWithMessage() {
        String result = aiCodeHelperService.chat("2", "你好我是小王");
        System.out.println(result);

        String result1 = aiCodeHelperService.chat("2", "你好，还记得我是谁吗");
        System.out.println(result1);
    }

    @Test
    void chatForReport() {
        AiCodeHelperService.Report msg = aiCodeHelperService.chatForReport("3", "你好我是小王");
        System.out.println(msg);
    }

    @Test
    void chatWithRag() {
        String result = aiCodeHelperService.chat("2", "如何学习java");
        System.out.println(result);
    }

    @Test
    void chatWithRag1() {
        Result<String> result = aiCodeHelperService.chatWthRag( "如何学习java");
        System.out.println(result.sources());
        System.out.println(result.content());
    }

    @Test
    void chatWithTool() {
        String result = aiCodeHelperService.chat( "5","有哪些常见的java面试题");
        System.out.println(result);
    }

    @Test
    void chatWithMcp() {
        String result = aiCodeHelperService.chat( "6","什么是程序员鱼皮的编程导航");
        System.out.println(result);
    }

    @Test
    void chatWithGuardrail() {
        String result = aiCodeHelperService.chat( "6","python如何学习呢");
        System.out.println(result);
    }

}