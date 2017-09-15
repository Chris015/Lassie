package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.TagSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.Log;
import lassie.config.Account;
import lassie.event.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3BucketTagger implements ResourceTagger {
    private static Logger log = Logger.getLogger(S3BucketTagger.class);
    private AmazonS3 s3;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateS3Client(log.getAccount());
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateS3Client(Account account) {
        log.info("Instantiating S3 client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(account.getAccessKeyId(), account.getSecretAccessKey());
        this.s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(account.getRegions().get(0)))
                .build();
        log.info("S3 client instantiated");
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'CreateBucket' && @.requestParameters != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("requestParameters")
                            .getAsJsonObject().get("bucketName")
                            .getAsString();
                    String owner = jsonElement.getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    log.info("S3 bucket event created. Id: " + id + " Owner: " + owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> runInstancesEvents = gson.fromJson(json, new TypeToken<List<Event>>() {
                }.getType());
                events.addAll(runInstancesEvents);
            } catch (IOException e) {
                log.error("Could not parse json: ", e);
                e.printStackTrace();
            }
        }
        log.info("Done parsing json");
    }

    private void filterTaggedResources(String ownerTag) {
        log.info("Filtering tagged Buckets");
        List<Event> untaggedBuckets = new ArrayList<>();
        for (Event event : events) {
            if (s3.getBucketTaggingConfiguration(event.getId()) == null) {
                return;
            }
            BucketTaggingConfiguration configuration = s3.getBucketTaggingConfiguration(event.getId());
            List<TagSet> allTagSets = configuration.getAllTagSets();
            for (TagSet tagSet : allTagSets) {
                if (!hasTag(tagSet, ownerTag)) {
                    untaggedBuckets.add(event);
                }
            }
        }
        log.info("Done filtering tagged Buckets");
        this.events = untaggedBuckets;
    }

    private boolean hasTag(TagSet tagSet, String tag) {
        log.trace(tag + " found: " +  tagSet.getAllTags().containsKey(tag));
        return tagSet.getAllTags().containsKey(tag);
    }

    private void tag(String ownerTag) {
        log.info("Tagging Buckets");
        Map<String, String> newTags = new HashMap<>();
        List<TagSet> tags = new ArrayList<>();
        for (Event event : events) {
            BucketTaggingConfiguration configuration = s3.getBucketTaggingConfiguration(event.getId());
            List<TagSet> oldTags;
            if (configuration != null) {
                oldTags = configuration.getAllTagSets();
                oldTags.forEach(oldTag -> newTags.putAll(oldTag.getAllTags()));
            }
            newTags.put(ownerTag, event.getOwner());
            tags.add(new TagSet(newTags));
            s3.setBucketTaggingConfiguration(event.getId(), new BucketTaggingConfiguration(tags));
            log.info("Tagged: " + event.getId()
                    + " with key: " + ownerTag
                    + " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging Buckets");
    }
}
