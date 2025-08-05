package org.example.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import dev.langchain4j.service.spring.AiService;
import org.example.ai.guardrail.SafeInputGuardrail;
import reactor.core.publisher.Flux;

import java.util.List;

//改为手动构建，更灵活
//@AiService
@InputGuardrails({SafeInputGuardrail.class})
public interface AiCodeHelperService {

    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(@MemoryId String  memoryId, @UserMessage String userMessage);
//    String chat(@UserMessage String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(@MemoryId String  memoryId, @UserMessage String userMessage);

    record Report(String message,List<String> stringList) {
    }

    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWthRag(String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId String memoryId, @UserMessage String userMessage);
}
