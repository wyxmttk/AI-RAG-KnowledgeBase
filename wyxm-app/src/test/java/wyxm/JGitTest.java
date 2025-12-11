package wyxm;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;
import wyxm.utils.GitUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class JGitTest {
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private PgVectorStore vectorStore;
    @Resource
    private ResourceLoader resourceLoader;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RAGController ragController;

    @Test
    public void testClone(){
        GitUtil.clone("git@github.com:wyxmttk/Tiny-Spring.git","data/code");
    }

    @Test
    public void testUploadGitRepository(){
        ragController.uploadGitRepository("Tiny-Spring2","git@github.com:wyxmttk/Tiny-Spring.git","");
    }

    @Test
    public void testUpload(){
        int maxDocuments=20;
        List<Document> buffer = new ArrayList<>();
        RList<Object> ragTag = redissonClient.getList("ragTag");
        if(!ragTag.contains("Tiny-Spring")) ragTag.add("Tiny-Spring");
        try {
            Files.walkFileTree(Paths.get("data/code"), new SimpleFileVisitor<>() {
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
                            metadata.put("knowledge","Tiny-Spring");
                            metadata.put("sourceFile",file.getFileName().toString());

                        });
                        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
                        for(Document document : splitDocuments){
                            buffer.add(document);
                            if(buffer.size()>=maxDocuments){
                                vectorStore.add(buffer);
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
            vectorStore.add(buffer);
            log.info("剩余数据上成功，数量:{}",buffer.size());
        }
    }

}
