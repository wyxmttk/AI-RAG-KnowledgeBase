package wyxm;

import org.springframework.web.multipart.MultipartFile;
import wyxm.response.Response;

import java.util.List;

public interface RAGService {

    Response<List<String>> queryRagTagList();

    Response<String> uploadFile(String ragTag, List<MultipartFile> files);

    Response<String> uploadGitRepository(String ragTag, String url);
}
