package lassie.mocks;

public class S3HandlerMock implements lassie.awshandlers.S3Handler {
    @Override
    public void instantiateS3Client(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagBucket(String bucketName, String key, String value) {

    }

    @Override
    public boolean bucketHasTag(String bucketName, String tag) {
        return false;
    }
}
