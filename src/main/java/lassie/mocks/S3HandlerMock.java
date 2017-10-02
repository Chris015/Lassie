package lassie.mocks;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class S3HandlerMock implements lassie.awshandlers.S3Handler {
    private final static Logger log = Logger.getLogger(S3HandlerMock.class);
    public static List<String> bucketsWithTag = new ArrayList<>();
    public static List<String> bucketsWithoutTag = new ArrayList<>();

    @Override
    public void instantiateS3Client(String accessKeyId, String secretAccessKey, String region) {
    }

    @Override
    public void tagBucket(String bucketName, String key, String value) {
        log.info("Tagged: " + bucketName + " with key: " + key + " value: " + value);
    }

    @Override
    public boolean bucketHasTag(String bucketName, String tag) {
        return bucketsWithTag.contains(bucketName);
    }
}
