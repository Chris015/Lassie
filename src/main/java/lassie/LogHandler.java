package lassie;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import lassie.config.Account;
import lassie.model.Log;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class LogHandler {
    private final static Logger log = Logger.getLogger(LogHandler.class);
    private AmazonS3 s3;
    private Path tmpFolderZipped;
    private Path tmpFolderUnzipped;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private String tmpFolder = "tmp";

    public LogHandler() {
        createTmpFolders();
    }

    public List<Log> getLogs(String startDate, List<Account> accounts) {
        List<Log> logs = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = LocalDate.parse(startDate);
        List<LocalDate> totalDates = new ArrayList<>();
        log.info("Getting logs for dates: " + startDate + " to " + end);

        for (Account account : accounts) {
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(account.getAccessKeyId(),
                    account.getSecretAccessKey());
            this.s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(Regions.fromName(account.getBucketRegion()))
                    .build();

            log.info("S3 Client for AWS logs download created");

            while (!start.isAfter(end)) {
                totalDates.add(start);
                start = start.plusDays(1);
            }

            for (LocalDate date : totalDates) {
                for (String region : account.getRegions()) {
                    List<S3ObjectSummary> summaries = getObjectSummaries(date.format(formatter), account, region);
                    List<String> filePaths = downloadLogs(account, summaries);
                    logs.add(createLog(account, region, filePaths));
                }
            }
        }
        log.info("Logs downloaded for all the given dates");
        return logs;
    }

    private List<S3ObjectSummary> getObjectSummaries(String date, Account account, String region) {
        log.debug("Getting object summaries from: "
                + account.getS3Url().getBucket() + "/"
                + account.getS3Url().getKey()
                + "/AWSLogs/"
                + account.getAccountId() + "/"
                + "CloudTrail/"
                + region + "/"
                + date + "/");
        log.info("Getting object summaries for date: " + date);
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(account.getS3Url().getBucket())
                .withPrefix(account.getS3Url().getKey()
                        + "/AWSLogs/"
                        + account.getAccountId() + "/"
                        + "CloudTrail/"
                        + region + "/"
                        + date + "/");
        log.debug("Get object summaries complete");
        return s3.listObjectsV2(request).getObjectSummaries();
    }

    private List<String> downloadLogs(Account account, List<S3ObjectSummary> summaries) {
        List<String> fileNames = downloadZip(account, summaries);
        return unzipObject(fileNames);
    }

    private List<String> downloadZip(Account account, List<S3ObjectSummary> summaries) {
        log.debug("Downloading zipped files");
        List<String> fileNames = new ArrayList<>();
        for (S3ObjectSummary objectSummary : summaries) {
            String key = objectSummary.getKey();
            try (S3Object s3Object = s3.getObject(new GetObjectRequest(account.getS3Url().getBucket(), key));
                 S3ObjectInputStream objectContent = s3Object.getObjectContent()) {

                String filename = s3Object.getKey().substring(key.lastIndexOf('/') + 1, key.length());
                log.debug("Downloading file: " + filename);
                Files.copy(objectContent,
                        Paths.get(tmpFolderZipped + "/" + filename),
                        StandardCopyOption.REPLACE_EXISTING);
                fileNames.add(filename);

            } catch (IOException e) {
                log.error("Could not download file: ", e);
                e.printStackTrace();
            }
        }
        log.debug("Download complete");
        return fileNames;
    }

    private List<String> unzipObject(List<String> filenames) {
        log.debug("Unzipping objects");
        List<String> filePaths = new ArrayList<>();
        for (String filename : filenames) {
        log.debug("Unzipping object: " + filename);
            String fileInputPath = tmpFolderZipped + "/" + filename;
            String fileOutputPath = tmpFolderUnzipped + "/" + filename.substring(0, filename.length() - 3);
            try (FileInputStream fileInputStream = new FileInputStream(fileInputPath);
                 GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
                 FileOutputStream fileOutputStream = new FileOutputStream(fileOutputPath)) {

                byte[] buffer = new byte[1024];
                int len;

                while ((len = gzipInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len);
                }

                filePaths.add(fileOutputPath);

            } catch (IOException e) {
                log.error("Could not unzip file: " + filename, e);
                e.printStackTrace();
            }
        }
        log.debug("Unzipping objects complete");
        return filePaths;
    }

    private Log createLog(Account account, String region, List<String> filePaths) {
        log.debug("Creating log");
        List<String> regions = new ArrayList<>();

        regions.add(region);
        Log logObject = new Log(
                new Account(account.getOwnerTag(),
                        account.getAccessKeyId(),
                        account.getSecretAccessKey(),
                        account.getAccountId(),
                        account.getS3Url(),
                        account.getBucketRegion(),
                        account.getResourceTypes(),
                        regions),
                filePaths);
        log.debug("Log created");
        return logObject;

    }

    private void createTmpFolders() {
        log.debug("Creating temp folders");
        try {
            if (!Files.isDirectory(Paths.get(tmpFolder))) {
                Files.createDirectory(Paths.get(tmpFolder));
            }

            tmpFolderZipped = Files.createTempDirectory(Paths.get(tmpFolder + "/"), null);
            tmpFolderUnzipped = Files.createTempDirectory(Paths.get(tmpFolder + "/"), null);

        } catch (IOException e) {
            log.error("Temp folders could not be created: ", e);
            e.printStackTrace();
        }
        log.info("Temp folders created");
    }

    public void clearLogs() {
        try {
            FileUtils.cleanDirectory(new File(tmpFolder));
            log.info("Temp-directory cleaned");
        } catch (IOException e) {
            log.error("Temp directory could not be cleaned: ", e);
            e.printStackTrace();
        }
    }
}
