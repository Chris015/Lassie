package lassie.mocks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class S3HandlerMock implements lassie.awshandlers.S3Handler {
    private final static Logger logger = LogManager.getLogger(S3HandlerMock.class);
    public static List<String> bucketsWithTag = new ArrayList<>();
    public static List<String> bucketsWithoutTag = new ArrayList<>();

    @Override
    public void instantiateS3Client(String accessKeyId, String secretAccessKey, String region) {
    }

    @Override
    public void tagBucket(String bucketName, String key, String value) {
        logger.info("Tagged: {} with key: {} value: {}", bucketName, key, value);
    }

    @Override
    public boolean bucketHasTag(String bucketName, String tag) {
        return bucketsWithTag.contains(bucketName);
    }
}
