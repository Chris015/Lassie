package lassie.resourcetagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awshandlers.S3Handler;
import lassie.config.Account;
import lassie.model.Event;
import lassie.model.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class S3BucketTagger implements ResourceTagger {
    private static final Logger logger = LogManager.getLogger(S3BucketTagger.class);
    private S3Handler s3Handler;
    private List<Event> events = new ArrayList<>();

    public S3BucketTagger(S3Handler s3Handler) {
        this.s3Handler = s3Handler;
    }

    @Override
    public void tagResources(Account account) {
        for (Log log : account.getLogs()) {
            logger.info("Trying to tag S3 buckets in region {} for date: {}", log.getRegion(), log.getDate());
            s3Handler.instantiateS3Client(account.getAccessKeyId(), account.getSecretAccessKey(), log.getRegion());
            parseJson(log.getFilePaths());
            filterEventsWithoutTag(account.getOwnerTag());
            tag(account.getOwnerTag());
        }
    }

    private void parseJson(List<String> filePaths) {
        logger.trace("Parsing json");
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
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> runInstancesEvents = gson.fromJson(json, new TypeToken<List<Event>>() {
                }.getType());
                events.addAll(runInstancesEvents);
            } catch (IOException e) {
                logger.error("Could not parse json: {} \nError: {}", filePath, e);
                e.printStackTrace();
            }
        }
        logger.info("Found: {} events in cloud-trail logs", events.size());
        events.forEach(event -> logger.info("Id: {} Owner: {}", event.getId(), event.getOwner()));
        logger.trace("Done parsing json");
    }

    private void filterEventsWithoutTag(String ownerTag) {
        logger.trace("Filtering Buckets without: {}", ownerTag);
        List<Event> untaggedBuckets = new ArrayList<>();
        for (Event event : events) {
            if (!s3Handler.bucketHasTag(event.getId(), ownerTag)) {
                untaggedBuckets.add(event);
            }
        }
        logger.trace("Done filtering Buckets");
        this.events = untaggedBuckets;
    }

    private void tag(String ownerTag) {
        logger.trace("Tagging Buckets");
        if (events.size() == 0) {
            logger.info("No untagged Buckets found in cloud-trail logs");
        }
        for (Event event : events) {
            s3Handler.tagBucket(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        logger.trace("Done tagging Buckets");
    }
}
