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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class S3LogFetcher implements LogFetcher {
    private static final Logger logger = LogManager.getLogger(S3LogFetcher.class);
    private AmazonS3 s3;
    private Path tmpFolderZipped;
    private Path tmpFolderUnzipped;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private String tmpFolder = "tmp";

    public void addLogsToAccount(String startDate, List<Account> accounts) {
        LocalDate end = LocalDate.now();
        LocalDate start = LocalDate.parse(startDate);
        List<LocalDate> totalDates = new ArrayList<>();
        logger.info("Getting logs for dates: {} to {}", start, end);

        for (Account account : accounts) {
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(account.getAccessKeyId(),
                    account.getSecretAccessKey());
            logger.info("Instantiating S3 client");
            this.s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .withRegion(Regions.fromName(account.getBucketRegion()))
                    .build();

            logger.info("S3 client instantiated");

            while (!start.isAfter(end)) {
                totalDates.add(start);
                start = start.plusDays(1);
            }

            for (LocalDate date : totalDates) {
                for (String region : account.getRegions()) {
                    List<S3ObjectSummary> summaries = getObjectSummaries(date.format(formatter), account, region);
                    List<String> filePaths = downloadLogs(account, summaries);
                    account.addLog(createLog(region, filePaths));
                }
            }
        }
        logger.info("Logs downloaded for all the given dates");
    }

    public void createTmpFolders() {
        logger.debug("Creating temp folders");
        try {
            if (!Files.isDirectory(Paths.get(tmpFolder))) {
                Files.createDirectory(Paths.get(tmpFolder));
            }

            tmpFolderZipped = Files.createTempDirectory(Paths.get(tmpFolder + "/"), null);
            logger.debug("TmpFolderZipped: {}", tmpFolderZipped);
            tmpFolderUnzipped = Files.createTempDirectory(Paths.get(tmpFolder + "/"), null);
            logger.debug("TmpFolderUnzipped: {}", tmpFolderUnzipped);
        } catch (IOException e) {
            logger.error("Temp folders could not be created: ", e);
            e.printStackTrace();
        }
        logger.info("Temp folders created");
    }

    public void clearLogs() {
        try {
            FileUtils.cleanDirectory(new File(tmpFolder));
            logger.info("Temp-directory cleaned");
        } catch (IOException e) {
            logger.error("Temp directory could not be cleaned: ", e);
            e.printStackTrace();
        }
    }

    private List<S3ObjectSummary> getObjectSummaries(String date, Account account, String region) {
        logger.info("Getting object summaries for date: {}", date);
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(account.getS3Url().getBucket())
                .withPrefix(account.getS3Url().getKey()
                        + "/AWSLogs/"
                        + account.getAccountId() + "/"
                        + "CloudTrail/"
                        + region + "/"
                        + date + "/");
        logger.debug("Get object summaries complete");
        return s3.listObjectsV2(request).getObjectSummaries();
    }

    private List<String> downloadLogs(Account account, List<S3ObjectSummary> summaries) {
        List<String> fileNames = downloadZip(account, summaries);
        return unzipObject(fileNames);
    }

    private List<String> downloadZip(Account account, List<S3ObjectSummary> summaries) {
        logger.debug("Downloading zipped files");
        List<String> fileNames = new ArrayList<>();
        for (S3ObjectSummary objectSummary : summaries) {
            String key = objectSummary.getKey();
            try (S3Object s3Object = s3.getObject(new GetObjectRequest(account.getS3Url().getBucket(), key));
                 S3ObjectInputStream objectContent = s3Object.getObjectContent()) {

                String filename = s3Object.getKey().substring(key.lastIndexOf('/') + 1, key.length());
                logger.debug("Downloading file: {}", filename);
                Files.copy(objectContent,
                        Paths.get(tmpFolderZipped + "/" + filename),
                        StandardCopyOption.REPLACE_EXISTING);
                fileNames.add(filename);
            } catch (IOException e) {
                logger.error("Could not download file: ", e);
                e.printStackTrace();
            }
        }
        logger.debug("Download complete");
        return fileNames;
    }

    private List<String> unzipObject(List<String> filenames) {
        logger.debug("Unzipping objects");
        List<String> filePaths = new ArrayList<>();
        for (String filename : filenames) {
            logger.debug("Unzipping object: {}", filename);
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
                logger.error("Could not unzip file: {}", filename, e);
                e.printStackTrace();
            }
        }
        logger.debug("Unzipping objects complete");
        return filePaths;
    }

    private Log createLog(String region, List<String> filePaths) {
        logger.debug("Creating log");
        Log log = new Log(region, filePaths);
        logger.debug("Log created");
        return log;

    }
}
