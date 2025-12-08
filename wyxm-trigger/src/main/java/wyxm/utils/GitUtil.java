package wyxm.utils;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

public class GitUtil {
    public static void clone(String url,String toDir) {
        File file = new File(toDir);
        if(file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            Git git = Git.cloneRepository().setURI(url).setDirectory(file).call();
            git.close();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }


}
