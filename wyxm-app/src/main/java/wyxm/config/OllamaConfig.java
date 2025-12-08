package wyxm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Slf4j
public class OllamaConfig {
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
    @Bean("ollamaPgVectorStore")
    public PgVectorStore pgVectorStore(@Value("${spring.ai.ollama.embedding.model}") String model
            , @Value("${spring.ai.ollama.vector-store-name-prefix}") String prefix
            , OllamaEmbeddingModel ollamaEmbeddingModel
            , JdbcTemplate jdbcTemplate) {
        return new PgVectorStore
                .Builder(jdbcTemplate,ollamaEmbeddingModel)
                .withVectorTableName(prefix+model.replace("-",""))
                .withInitializeSchema(true)
                .build();
    }

}
