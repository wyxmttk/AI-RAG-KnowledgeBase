package wyxm;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class MCPTest {

    @Resource
    private ZhiPuAiChatModel zhiPuAiChatModel;

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @Test
    public void testChat(){
        ChatClient.CallResponseSpec response = chatClientBuilder.defaultTools(toolCallbackProvider).build().prompt("有哪些工具可以使用").call();
        log.info(response.content());
    }
    @Test
    public void testOperation(){
        ChatClient.CallResponseSpec response = chatClientBuilder.defaultTools(toolCallbackProvider).build().prompt("把app模块的test.txt文件删掉，在trigger模块下创建test.txt").call();
        log.info(response.content());
    }
    @Test
    public void testComputerInfo(){
        ChatClient.CallResponseSpec response = chatClientBuilder.defaultTools(toolCallbackProvider).build().prompt("我当前电脑的信息是？").call();
        log.info(response.content());
    }
    @Test
    public void testWorkFlow(){
        ChatClient.CallResponseSpec response = chatClientBuilder.defaultTools(toolCallbackProvider).build().prompt("把我当前电脑的配置信息写到app模块的test.txt文件").call();
        log.info(response.content());
    }
}
