package wyxm;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wyxm.response.Response;
import wyxm.utils.GitUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rag/")
@CrossOrigin("*")
@Slf4j
public class RAGController implements RAGService {
    @Resource(name = "zaiPgVectorStore")
    private PgVectorStore zaiPgVectorStore;
    @Resource(name = "ollamaPgVectorStore")
    private PgVectorStore ollamaPgVectorStore;
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
    public Response<String> uploadFile(String ragTag, List<MultipartFile> files, String model) {
        PgVectorStore pgVectorStore;
        if("ZhiPuAI".equals(model)) {
            pgVectorStore = zaiPgVectorStore;
        }else if("ollama".equals(model)) {
            pgVectorStore = ollamaPgVectorStore;
        }else throw new IllegalArgumentException();
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

    @GetMapping("git")
    @Override
    public Response<String> uploadGitRepository(String tag, String url,String model) {
        PgVectorStore pgVectorStore;
        if("ZhiPuAI".equals(model)) {
            pgVectorStore = zaiPgVectorStore;
        }else if("ollama".equals(model)) {
            pgVectorStore = ollamaPgVectorStore;
        }else throw new IllegalArgumentException();
        String toDir = String.format("data/code/%s/", UUID.randomUUID());
        GitUtil.clone(url,toDir );
        int maxDocuments=20;
        List<Document> buffer = new ArrayList<>();
        RList<Object> ragTag = redissonClient.getList("ragTag");
        if(!ragTag.contains(tag)) ragTag.add(tag);
        try {
            Files.walkFileTree(Paths.get(toDir), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                    if (dir.getFileName().toString().startsWith(".")|| dir.getFileName().toString().equals("target")) {
                        log.info("跳过目录:{}", dir.getFileName());
                        return FileVisitResult.SKIP_SUBTREE; // 直接跳过整个子目录，不要进去遍历
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(Files.isHidden(file) || file.getFileName().toString().startsWith(".")){
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        PathResource pathResource = new PathResource(file);
                        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(pathResource);
                        List<Document> documents = tikaDocumentReader.get();
                        documents.forEach(document -> {
                            Map<String, Object> metadata = document.getMetadata();
                            metadata.put("knowledge",tag);

                        });
                        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
                        for(Document document : splitDocuments){
                            buffer.add(document);
                            if(buffer.size()>=maxDocuments){
                                pgVectorStore.add(buffer);
                                log.info("批次上传成功，数量:{}", buffer.size());
                                buffer.clear();
                            }
                        }
                    } catch (Exception e) {
                        log.error("上传文件失败，跳过:{}",file.getFileName().toString());
                        e.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if(!buffer.isEmpty()){
            pgVectorStore.add(buffer);
            log.info("剩余数据上成功，数量:{}",buffer.size());
        }
        try {
            FileUtils.deleteDirectory(new File(toDir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Response.success();
    }


}
