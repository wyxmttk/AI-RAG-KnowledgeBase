package wyxm;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
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

    public static final String SYSTEM_PROMPT= """
        你是一个智能助手。请严格基于以下提供的上下文信息回答用户的问题。
        严禁使用你原本的训练数据或外部知识来回答。
        如果上下文信息中没有包含问题的答案，必须回答：“抱歉，知识库中未找到相关内容。”，不要说其他废话。
    
        [上下文开始]
        {documents}
        [上下文结束]
        """;

    @Resource
    private OllamaChatModel chatClient;

    @Resource
    private PgVectorStore pgVectorStore;

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

    @GetMapping("generate_stream_rag")
    @Override
    public Flux<ChatResponse> streamingGeneratingRAG(String model, String ragTag, String message) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        SearchRequest request=SearchRequest.query(message).withTopK(5).withFilterExpression(builder.eq("knowledge", ragTag).build());
        List<Document> documents = pgVectorStore.similaritySearch(request);
        List<Message> messages = new ArrayList<>();
        Message systemPrompt = new SystemPromptTemplate(SYSTEM_PROMPT)
                .createMessage(Map.of("documents",documents.stream().map(Document::getContent).collect(Collectors.joining())));
        Message userPrompt = new UserMessage(message);
        messages.add(systemPrompt);
        messages.add(userPrompt);
        return chatClient.stream(new Prompt(messages, OllamaOptions.create().withModel(model)));
    }


}
