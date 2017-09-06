package lassie;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lassie.config.S3Url;
import lassie.event.Event;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

public class LogPersister {

    private Path tmpFolderZipped;
    private Path tmpFolderUnzipped;
    private S3Object s3Object;
    private S3ObjectInputStream objectContent;
    private AmazonS3 s3;
    private S3Url s3Url;
    private DateFormatter dateFormatter;

    public LogPersister(AmazonS3 s3, S3Url s3Url, DateFormatter dateFormatter) {
        this.s3 = s3;
        this.s3Url = s3Url;
        this.dateFormatter = dateFormatter;
    }

    public List<S3ObjectSummary> listObjects(List<Event> events) {
        for (Event event : events) {
            String date = "2017/08/31";

            if (event.getLaunchTime() != 0) {
                date = dateFormatter.format(event.getLaunchTime());
            }

            ListObjectsV2Request req = new ListObjectsV2Request()
                    .withBucketName(s3Url.getBucket())
                    .withPrefix(s3Url.getKey()
                            + "/AWSLogs/"
                            + event.getOwnerId() + "/"
                            + "CloudTrail/"
                            + s3.getRegionName() + "/"
                            + date + "/");
            ListObjectsV2Result listing = s3.listObjectsV2(req);
            System.out.println(listing.getObjectSummaries().size());
            return listing.getObjectSummaries();
        }

        return null;
    }

    public void downloadObject(List<S3ObjectSummary> objectSummaries) {
        createTmpFolders();
        for (S3ObjectSummary objectSummary : objectSummaries) {
            String key = objectSummary.getKey();
            try {
                s3Object = s3.getObject(new GetObjectRequest(s3Url.getBucket(), key));
                objectContent = s3Object.getObjectContent();

                String filename = s3Object.getKey().substring(key.lastIndexOf('/') + 1, key.length());


                Files.copy(objectContent, Paths.get(tmpFolderZipped + "/" + filename), StandardCopyOption.REPLACE_EXISTING);
                unzipObject(filename);
                objectContent.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createTmpFolders() {
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

    public List<String> fetchUnzippedFiles() {

        List<String> jsonFiles = new ArrayList<>();

        File dir = new File(tmpFolderUnzipped.toString());
        File[] directoryListing = dir.listFiles();

        try {
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    String json = new Scanner(child).useDelimiter("\\Z").next();
                    if (!child.isHidden()) {
                        jsonFiles.add(json);
                    }
                }
            }
            return jsonFiles;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void deleteFolders() {
        try {
            if (Files.isDirectory(Paths.get("tmp"))) {
                FileUtils.cleanDirectory(new File("tmp"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
