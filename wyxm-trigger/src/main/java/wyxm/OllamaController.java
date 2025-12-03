package wyxm;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/ollama/")
public class OllamaController implements AIService {

    @Resource
    private OllamaChatClient chatClient;

//    http://localhost:8090/api/v1/ollama/generate?model=qwen2.5:7b&message=%E4%BD%A0%E6%98%AF%E4%BB%80%E4%B9%88%E6%A8%A1%E5%9E%8B
    @RequestMapping(value = "generate", method = RequestMethod.GET)
    @Override
    public ChatResponse generate(@RequestParam("model") String model, @RequestParam("message") String message) {
        return chatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
    }

//    http://localhost:8090/api/v1/ollama/generate_stream?model=qwen2.5:7b&message=%E4%BD%A0%E6%98%AF%E4%BB%80%E4%B9%88%E6%A8%A1%E5%9E%8B
    @RequestMapping(value = "generate_stream", method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> streamingGenerating(@RequestParam("model") String model, @RequestParam("message") String message) {
        return chatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }


}
