package wyxm;

import jakarta.annotation.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.Media;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ZAITest {

    @Resource(name="zhiPuAiEmbeddingModel")
    private EmbeddingModel embeddingClient;
    @Resource(name="zaiPgVectorStore")
    private PgVectorStore zaiPgVectorStore;
    @Resource
    private ResourceLoader resourceLoader;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ZhiPuAiChatModel zhiPuAiChatModel;

    @Test
    public void testApi(){
        RList<Object> ragTag = redissonClient.getList("ragTag");
        ragTag.clear();
        ragTag.add("测试知识库");
        org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:data/file.txt");
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();
        documents.forEach(document -> {document.getMetadata().put("knowledge","测试知识库");});
        List<Document> split = tokenTextSplitter.split(documents);
        zaiPgVectorStore.add(split);
    }
    @Test
    public void testChat(){
        String message="ljk指的是谁?";
        String filterExpression="knowledge == '测试知识库'";

        String SYSTEM_PROMPT= """
        你是一个智能助手。请严格基于以下提供的上下文信息回答用户的问题。
        严禁使用你原本的训练数据或外部知识来回答。
        如果上下文信息中没有包含问题的答案，必须回答：“抱歉，知识库中未找到相关内容。”，不要说其他废话。
    
        [上下文开始]
        {documents}
        [上下文结束]
        """;
        SearchRequest request=SearchRequest.builder().query(message).topK(5).filterExpression(filterExpression).build();
        List<Document> list = zaiPgVectorStore.similaritySearch(request);
        if(list==null){
            list=new ArrayList<>();
        }
        String collect = list.stream().map(Document::getText).collect(Collectors.joining());
        Message systemPrompt = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", collect));
        UserMessage userPrompt = new UserMessage(message);
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(systemPrompt);
        messages.add(userPrompt);
        ChatResponse response = zhiPuAiChatModel.call(new Prompt(messages));
        System.out.println(response);
    }
    @Test
    public void testImageParser(){
        ClassPathResource classPathResource = new ClassPathResource("data/img.png");
        UserMessage userMessage = new UserMessage("这张图片的内容是什么"
                , new Media(MimeType.valueOf(MimeTypeUtils.IMAGE_PNG_VALUE), classPathResource));
        Flux<String> stream = zhiPuAiChatModel.stream(userMessage);
        stream.filter(StringUtils::hasLength).doOnNext(System.out::println).blockLast();

    }
}
