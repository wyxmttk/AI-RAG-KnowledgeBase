package wyxm;

import org.springframework.ai.chat.ChatResponse;
import reactor.core.publisher.Flux;

public interface AIService {
    ChatResponse generate(String model,String message);

    Flux<ChatResponse> streamingGenerating(String model,String message);
}
