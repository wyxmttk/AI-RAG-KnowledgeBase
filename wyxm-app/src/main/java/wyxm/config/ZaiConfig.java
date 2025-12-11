package wyxm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ZaiConfig {

    @Bean("zaiPgVectorStore")
    public PgVectorStore pgVectorStore(@Value("${spring.ai.zhipuai.embedding.options.model}") String model
            , ZhiPuAiEmbeddingModel zhiPuAiEmbeddingModel
            , @Value("${spring.ai.zhipuai.vector-store-name-prefix}") String prefix
            , JdbcTemplate jdbcTemplate) {
        return PgVectorStore.builder(jdbcTemplate, zhiPuAiEmbeddingModel)
                .vectorTableName(prefix+model.replace("-",""))
                .initializeSchema(true)
                .build();
    }

    @Bean("zaiChatClient")
    public ChatClient.Builder chatClientBuilder(ZhiPuAiChatModel zhiPuAiChatModel, ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(zhiPuAiChatModel);
    }
}
