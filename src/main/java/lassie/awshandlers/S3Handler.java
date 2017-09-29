package lassie.awshandlers;

public interface S3Handler {
    void instantiateS3Client(String accessKeyId, String secretAccessKey, String region);

    void tagBucket(String bucketName, String key, String value);

    boolean bucketHasTag(String bucketName, String tag);
}
