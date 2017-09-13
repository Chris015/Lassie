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
import lassie.event.Event;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RDSDBInstanceTagger implements ResourceTagger {
    private AmazonRDS rds;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateRDSClient(log);
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateRDSClient(Log log) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(log.getAccount().getAccessKeyId(),
                log.getAccount().getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        rds = AmazonRDSClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(log.getAccount().getRegions().get(0))
                .build();
    }

    private void parseJson(List<String> filePaths) {
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
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createDBInstanceEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createDBInstanceEvents);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void filterTaggedResources(String ownerTag) {
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
        this.events = untaggedEvents;
    }

    private List<DBInstance> describeRDSInstances(String ownerTag) {
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
        return dbInstancesWithoutOwner;
    }

    private boolean hasTag(ListTagsForResourceResult response, String tag) {
        return response.getTagList().stream().anyMatch(t -> t.getKey().equals(tag));
    }

    private void tag(String ownerTag) {
        for (Event event : events) {
            Tag tag = new Tag();
            tag.setKey(ownerTag);
            tag.setValue(event.getOwner());
            AddTagsToResourceRequest tagsRequest = new AddTagsToResourceRequest()
                    .withResourceName(event.getId())
                    .withTags(tag);
            rds.addTagsToResource(tagsRequest);
            System.out.println("Tagged: " + event.getId() +
                    " with key: " + ownerTag +
                    " value: " + event.getOwner());
        }
    }

}
