package lassie.AWSHandlers;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.TagSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3Handler {
    private AmazonS3 s3;

    public void instantiateS3(AmazonS3 s3) {
        this.s3 = s3;
    }

    public void tagBucket(String bucketName, Tag tag) {
        Map<String, String> newTags = new HashMap<>();

        List<TagSet> existingTagSets = fetchExistingTagSets(bucketName);
        existingTagSets.forEach(existingTagSet -> newTags.putAll(existingTagSet.getAllTags()));

        List<TagSet> newTagSets = new ArrayList<>();
        newTags.put(tag.getKey(), tag.getValue());
        newTagSets.add(new TagSet(newTags));

        s3.setBucketTaggingConfiguration(bucketName, new BucketTaggingConfiguration(newTagSets));
    }

    private List<TagSet> fetchExistingTagSets(String bucketName) {
        List<TagSet> existingTagSets = new ArrayList<>();
        BucketTaggingConfiguration configuration = s3.getBucketTaggingConfiguration(bucketName);
        if (configuration != null) {
            existingTagSets = configuration.getAllTagSets();
        }
        return existingTagSets;
    }

    public boolean bucketHasTag(String bucketName, String ownerTag) {
        if (s3.getBucketTaggingConfiguration(bucketName) == null) {
            return false;
        }
        BucketTaggingConfiguration configuration = s3.getBucketTaggingConfiguration(bucketName);
        List<TagSet> allTagSets = configuration.getAllTagSets();
        for (TagSet tagSet : allTagSets) {
            if (tagSet.getAllTags().containsKey(ownerTag)) {
                return true;
            }

        }
        return false;
    }
}