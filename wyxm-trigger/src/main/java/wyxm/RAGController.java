package wyxm;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wyxm.response.Response;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rag/")
@CrossOrigin("*")
@Slf4j
public class RAGController implements RAGService {

    @Resource
    private PgVectorStore pgVectorStore;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @GetMapping("list")
    @Override
    public Response<List<String>> queryRagTagList() {
        return Response.success(redissonClient.getList("ragTag"));
    }
    @PostMapping("upload")
    @Override
    public Response<String> uploadFile(String ragTag, List<MultipartFile> files) {
        for(MultipartFile file : files) {
            TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
            List<Document> documents = reader.get();
            documents.forEach(document -> {document.getMetadata().put("knowledge",ragTag);});
            List<Document> splitDocuments = tokenTextSplitter.apply(documents);
            pgVectorStore.add(splitDocuments);
        }
        RList<Object> list = redissonClient.getList("ragTag");
        if(!list.contains(ragTag)) {
            list.add(ragTag);
        }
        log.info("文件上传完成:{}",ragTag);
        return Response.success();
    }


}
