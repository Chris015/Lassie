package lassie.awshandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.TagSet;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lassie.Application.DRY_RUN;

public class S3HandlerImpl implements S3Handler {
    private static final Logger log = Logger.getLogger(S3HandlerImpl.class);
    private AmazonS3 s3;

    public void instantiateS3Client(String accessKeyId, String secretAccessKey, String region) {
        log.info("Instantiating S3 client");
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        this.s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.fromName(region))
                .build();
        log.info("S3 client instantiated");
    }

    public void tagBucket(String bucketName, String key, String value) {
        if (DRY_RUN) {
            log.info("Dry run: " + DRY_RUN + ". Did not tag: " + bucketName + " with " + key + ": " + value);
            return;
        }
        Map<String, String> newTags = new HashMap<>();

        List<TagSet> existingTagSets = fetchExistingTagSets(bucketName);
        existingTagSets.forEach(existingTagSet -> newTags.putAll(existingTagSet.getAllTags()));

        List<TagSet> newTagSets = new ArrayList<>();
        newTags.put(key, value);
        newTagSets.add(new TagSet(newTags));

        s3.setBucketTaggingConfiguration(bucketName, new BucketTaggingConfiguration(newTagSets));
        log.info("Tagged: " + bucketName + " with key: " + key + " value: " + value);
    }

    public boolean bucketHasTag(String bucketName, String tag) {

        if (s3.getBucketTaggingConfiguration(bucketName) == null) {
            return false;
        }

        BucketTaggingConfiguration configuration = s3.getBucketTaggingConfiguration(bucketName);
        List<TagSet> allTagSets = configuration.getAllTagSets();
        for (TagSet tagSet : allTagSets) {
            if (tagSet.getAllTags().containsKey(tag)) {
                return true;
            }
        }
        return false;
    }

    private List<TagSet> fetchExistingTagSets(String bucketName) {
        List<TagSet> existingTagSets = new ArrayList<>();
        BucketTaggingConfiguration configuration = s3.getBucketTaggingConfiguration(bucketName);
        if (configuration != null) {
            existingTagSets = configuration.getAllTagSets();
        }
        return existingTagSets;
    }
}