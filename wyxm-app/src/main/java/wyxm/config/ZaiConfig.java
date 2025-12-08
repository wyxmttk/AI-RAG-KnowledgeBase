package wyxm.config;

import org.springframework.ai.vectorstore.PgVectorStore;
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
        return new PgVectorStore
                .Builder(jdbcTemplate, zhiPuAiEmbeddingModel)
                .withVectorTableName(prefix+model.replace("-",""))
                .withInitializeSchema(true)
                .build();
    }
}
