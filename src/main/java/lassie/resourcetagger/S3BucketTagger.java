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
import lassie.event.Event;
import lassie.event.RunInstances;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class S3BucketTagger implements ResourceTagger {
    private AmazonS3 s3;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateS3Client(log);
            parseJson(log);
            filterBuckets(log);
            tag(log);
        }
    }

    private void instantiateS3Client(Log log) {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(log.getAccount().getAccessKeyId(),
                log.getAccount().getSecretAccessKey());

        this.s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.fromName(log.getAccount().getBucketRegion()))
                .build();
    }

    private void parseJson(Log log) {
        try {
            String json = JsonPath.parse(new File(log.getFilePath()))
                    .read("$..Records[?(@.eventName == 'CreateBucket' && @.requestParameters != null)]").toString();
            GsonBuilder gsonBuilder = new GsonBuilder();
            JsonDeserializer<RunInstances> deserializer = (jsonElement, type, context) -> {
                String id = jsonElement
                        .getAsJsonObject().get("requestParameters")
                        .getAsJsonObject().get("bucketName").getAsString();
                String owner = jsonElement.getAsJsonObject().get("userIdentity").getAsJsonObject().get("arn").getAsString();
                return new RunInstances(id, owner);
            };

            gsonBuilder.registerTypeAdapter(RunInstances.class, deserializer);

            Gson gson = gsonBuilder.setLenient().create();
            List<RunInstances> runInstances = gson.fromJson(json, new TypeToken<List<RunInstances>>() {
            }.getType());
            events.addAll(runInstances);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void filterBuckets(Log log) {
        List<Event> untaggedBuckets = new ArrayList<>();
        for (Event event : events) {
            if (s3.getBucketTaggingConfiguration(event.getId()) == null) {
                return;
            }
            BucketTaggingConfiguration configuration = s3.getBucketTaggingConfiguration(event.getId());
            List<TagSet> allTagSets = configuration.getAllTagSets();
            for (TagSet allTagSet : allTagSets) {
                if (!allTagSet.getAllTags().containsKey(log.getAccount().getOwnerTag())) {
                    untaggedBuckets.add(event);
                }
            }
        }
        this.events = untaggedBuckets;
    }

    private void tag(Log log) {
        Map<String, String> newTags = new HashMap<>();
        List<TagSet> tags = new ArrayList<>();
        for (Event event : events) {
            BucketTaggingConfiguration configuration = s3.getBucketTaggingConfiguration(event.getId());
            List<TagSet> oldTags = configuration.getAllTagSets();
            oldTags.forEach(oldTag -> newTags.putAll(oldTag.getAllTags()));
            newTags.put(log.getAccount().getOwnerTag(), event.getOwner());
            tags.add(new TagSet(newTags));
            s3.setBucketTaggingConfiguration(event.getId(), new BucketTaggingConfiguration(tags));
        }
    }
}
