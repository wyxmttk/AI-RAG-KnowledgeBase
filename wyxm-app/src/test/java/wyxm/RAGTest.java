package wyxm;

import jakarta.annotation.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//测试时启动Spring上下文
@RunWith(SpringRunner.class)
@SpringBootTest
public class RAGTest {

    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private PgVectorStore vectorStore;
    @Resource
    private ResourceLoader resourceLoader;

    @Test
    public void upload(){
        org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:data/file.txt");
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
        List<Document> documents = tikaDocumentReader.get();
        documents.forEach(document -> {document.getMetadata().put("knowledge","测试知识库");});
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
        vectorStore.add(splitDocuments);
    }
    @Test
    public void chat(){
        String message="罗景康今年几岁?";
        String filterExpression="knowledge == '测试知识库'";

        String SYSTEM_PROMPT= """
        你是一个智能助手。请严格基于以下提供的上下文信息回答用户的问题。
        严禁使用你原本的训练数据或外部知识来回答。
        如果上下文信息中没有包含问题的答案，必须回答：“抱歉，知识库中未找到相关内容。”，不要说其他废话。
    
        [上下文开始]
        {documents}
        [上下文结束]
        """;
        SearchRequest request=SearchRequest.query(message).withTopK(5).withFilterExpression(filterExpression);
        List<Document> documents = vectorStore.similaritySearch(request);
        String documentsContent = documents.stream().map(Document::getContent).collect(Collectors.joining());
        Message systemMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsContent));
        UserMessage userMessage = new UserMessage(message);
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        ChatResponse response = ollamaChatClient.call(new Prompt(messages, OllamaOptions.create().withModel("qwen2.5:7b")));
        System.out.println(response);

    }


}
