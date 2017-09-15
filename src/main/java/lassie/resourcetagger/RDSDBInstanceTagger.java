package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.*;
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
import java.util.List;

public class RDSDBInstanceTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(RDSDBInstanceTagger.class);
    private AmazonRDS rds;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateRDSClient(log.getAccount());
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateRDSClient(Account account) {
        log.info("Instantiating RDS client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(account.getAccessKeyId(),
                account.getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        rds = AmazonRDSClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(account.getRegions().get(0))
                .build();
        log.info("RDS client created");
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'CreateDBInstance' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("dBInstanceArn")
                            .getAsString();
                    String owner = jsonElement
                            .getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    log.info("RDS DB instance event created. Id: " + id + " Owner: " + owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createDBInstanceEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createDBInstanceEvents);
            } catch (IOException e) {
                log.error("Could not parse json", e);
                e.printStackTrace();
            }
        }
        log.info("Parsing json complete");
    }

    private void filterTaggedResources(String ownerTag) {
        log.info("Filtering tagged DB instances");
        List<Event> untaggedEvents = new ArrayList<>();
        List<DBInstance> dbInstancesWithoutTag = describeRDSInstances(ownerTag);
        for (DBInstance dbInstance : dbInstancesWithoutTag) {
            for (Event event : events) {
                String dbId = dbInstance.getDBInstanceArn();
                String eventId = event.getId();
                if (dbId.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        log.info("Done filtering tagged DB instances");
        this.events = untaggedEvents;
    }

    private List<DBInstance> describeRDSInstances(String ownerTag) {
        log.info("Describing DB instances");
        List<DBInstance> dbInstancesWithoutOwner = new ArrayList<>();
        DescribeDBInstancesResult describeDBInstancesResult = rds.describeDBInstances(new DescribeDBInstancesRequest());
        for (DBInstance dbInstance : describeDBInstancesResult.getDBInstances()) {
            ListTagsForResourceRequest request = new ListTagsForResourceRequest()
                    .withResourceName(dbInstance.getDBInstanceArn());
            ListTagsForResourceResult response = rds.listTagsForResource(request);
            if (!hasTag(response, ownerTag)) {
                dbInstancesWithoutOwner.add(dbInstance);
            }
        }
        log.info("Found " + dbInstancesWithoutOwner.size() + " DB instances without tag");
        return dbInstancesWithoutOwner;
    }

    private boolean hasTag(ListTagsForResourceResult response, String tag) {
        return response.getTagList().stream().anyMatch(t -> t.getKey().equals(tag));
    }

    private void tag(String ownerTag) {
        log.info("Tagging DB instances");
        for (Event event : events) {
            Tag tag = new Tag();
            tag.setKey(ownerTag);
            tag.setValue(event.getOwner());
            AddTagsToResourceRequest tagsRequest = new AddTagsToResourceRequest()
                    .withResourceName(event.getId())
                    .withTags(tag);
            rds.addTagsToResource(tagsRequest);
            log.info("Tagged: " + event.getId() +
                    " with key: " + ownerTag +
                    " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging DB instances");
    }
}
