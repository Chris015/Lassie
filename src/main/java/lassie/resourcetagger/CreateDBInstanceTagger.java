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
import lassie.event.CreateDBInstance;
import lassie.event.Event;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateDBInstanceTagger implements ResourceTagger {
    private AmazonRDS rds;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        System.out.println("Parsing logs and tagging resources...");
        for (Log log : logs) {
            instantiateRDSClient(log);
            parseJson(log);
            filterTaggedResources(log);
            tag(log);
        }
    }

    public void instantiateRDSClient(Log log) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(log.getAccount().getAccessKeyId(), log.getAccount().getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        rds = AmazonRDSClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(log.getAccount().getRegions().get(0))
                .build();
    }

    private void parseJson(Log log) {
        try {
            String json = JsonPath.parse(new File(log.getFilePath()))
                    .read("$..Records[?(@.eventName == 'CreateDBInstance' && @.responseElements != null)]")
                    .toString();
            GsonBuilder gsonBuilder = new GsonBuilder();
            JsonDeserializer<CreateDBInstance> deserializer = (jsonElement, type, context) -> {
                String id = jsonElement
                        .getAsJsonObject().get("responseElements")
                        .getAsJsonObject().get("dBInstanceArn").getAsString();

                String owner = jsonElement.getAsJsonObject().get("userIdentity")
                        .getAsJsonObject().get("arn").getAsString();

                return new CreateDBInstance(id, owner);
            };
            gsonBuilder.registerTypeAdapter(CreateDBInstance.class, deserializer);
            Gson gson = gsonBuilder.setLenient().create();
            List<CreateDBInstance> createDBInstances = gson.fromJson(
                    json, new TypeToken<List<CreateDBInstance>>() {
                    }.getType());
            events.addAll(createDBInstances);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void filterTaggedResources(Log log) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<DBInstance> dbInstancesWithoutTag = describeRDSInstances(log);

        for (DBInstance tag : dbInstancesWithoutTag) {
            for (Event event : events) {
                String dbId = tag.getDBInstanceArn();
                String eventId = event.getId();
                if (dbId.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        this.events = untaggedEvents;
    }

    public List<DBInstance> describeRDSInstances(Log log) {
        List<DBInstance> dbInstancesWithoutOwner = new ArrayList<>();
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest();
        DescribeDBInstancesResult describeDBInstancesResult = rds.describeDBInstances(describeDBInstancesRequest);
        for (DBInstance dbInstance : describeDBInstancesResult.getDBInstances()) {
            ListTagsForResourceRequest request = new ListTagsForResourceRequest().withResourceName(dbInstance.getDBInstanceArn());
            ListTagsForResourceResult response = rds.listTagsForResource(request);
            if (response.getTagList().stream().noneMatch(t -> t.getKey().equals(log.getAccount().getOwnerTag()))) {
                dbInstancesWithoutOwner.add(dbInstance);
            }
        }
        return dbInstancesWithoutOwner;
    }

    private void tag(Log log) {
        for (Event event : events) {
            Tag tag = new Tag();
            tag.setKey(log.getAccount().getOwnerTag());
            tag.setValue(event.getOwner());
            AddTagsToResourceRequest tagsRequest = new AddTagsToResourceRequest()
                    .withResourceName(event.getId())
                    .withTags(tag);
            rds.addTagsToResource(tagsRequest);
            System.out.println("Tagged: " + event.getId() +
                    " with key: " + log.getAccount().getOwnerTag() +
                    " value: " + event.getOwner());
        }
    }
}
