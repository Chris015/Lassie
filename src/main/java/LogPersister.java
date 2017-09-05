import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

public class LogPersister {

    private Path tmpFolderZipped;
    private Path tmpFolderUnzipped;
    private S3Object s3Object;
    private S3ObjectInputStream objectContent;
    private AmazonS3 amazonS3;

    public LogPersister(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;

    }

    public void createTmpFolders() {
        try {
            if (!Files.isDirectory(Paths.get("tmp"))) {
                Files.createDirectory(Paths.get("tmp"));
            }

            tmpFolderZipped = Files.createTempDirectory(Paths.get("tmp/"), null);
            tmpFolderUnzipped = Files.createTempDirectory(Paths.get("tmp/"), null);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadObject(String bucketName, String key) {
        try {
            s3Object = amazonS3.getObject(new GetObjectRequest(bucketName, key));
            objectContent = s3Object.getObjectContent();

            String filename = s3Object.getKey().substring(key.lastIndexOf('/') + 1, key.length());


            Files.copy(objectContent, Paths.get(tmpFolderZipped + "/" + filename), StandardCopyOption.REPLACE_EXISTING);
            unzipObject(filename);
            objectContent.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void unzipObject(String filename) {
        try (FileInputStream fileInputStream = new FileInputStream(tmpFolderZipped + "/" + filename);
             GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
             FileOutputStream fileOutputStream = new FileOutputStream(
                     tmpFolderUnzipped + "/" + filename.substring(0, filename.length() - 3))) {

            byte[] buffer = new byte[1024];
            int len;

            while ((len = gzipInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFolders() {
        try {
            if (Files.isDirectory(Paths.get("tmp"))) {
                FileUtils.cleanDirectory(new File("tmp"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
