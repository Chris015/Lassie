package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Tag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.AWSHandlers.S3Handler;
import lassie.model.Log;
import lassie.config.Account;
import lassie.model.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class S3BucketTagger implements ResourceTagger {
    private static Logger log = Logger.getLogger(S3BucketTagger.class);
    private S3Handler s3Handler;
    private List<Event> events = new ArrayList<>();

    public S3BucketTagger(S3Handler s3Handler) {
        this.s3Handler = s3Handler;
    }

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
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(account.getAccessKeyId(), account.getSecretAccessKey());
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.fromName(account.getRegions().get(0)))
                .build();
        s3Handler.instantiateS3(s3);
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
                    log.info("S3 bucket model created. Id: " + id + " Owner: " + owner);
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
            if (!s3Handler.bucketHasTag(event.getId(), ownerTag)) {
                untaggedBuckets.add(event);
            }
        }
        log.info("Done filtering tagged Buckets");
        this.events = untaggedBuckets;
    }

    private void tag(String ownerTag) {
        log.info("Tagging Buckets");

        for (Event event : events) {

            s3Handler.tagBucket(event.getId(), new Tag(ownerTag, event.getOwner()));

            log.info("Tagged: " + event.getId()
                    + " with key: " + ownerTag
                    + " value: " + event.getOwner());

        }
        this.events = new ArrayList<>();
        log.info("Done tagging Buckets");
    }
}
