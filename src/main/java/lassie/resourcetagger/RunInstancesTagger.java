package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.Log;
import lassie.event.Event;
import lassie.event.RunInstances;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RunInstancesTagger implements ResourceTagger {
    private AmazonEC2 ec2;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateEc2Client(log);
            parseJson(log);
            filterTaggedResources(log);
            tag(log);
        }
    }

    private void instantiateEc2Client(Log log) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(log.getAccount().getAccessKeyId(), log.getAccount().getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(log.getAccount().getRegions().get(0))
                .build();
    }

    private void tag(Log log) {
        for (Event event : events) {
            CreateTagsRequest tagsRequest = new CreateTagsRequest()
                    .withResources(event.getId())
                    .withTags(new Tag(log.getAccount().getOwnerTag(), event.getOwner()));
            ec2.createTags(tagsRequest);
            System.out.println("Tagged: " + event.getId() +
                    " with key: " + log.getAccount().getOwnerTag() +
                    " value: " + event.getOwner());
        }

    }

    private void parseJson(Log log) {
        try {
            String json = JsonPath.parse(new File(log.getFilePath())).read("$..Records[?(@.eventName=='RunInstances')]").toString();
            GsonBuilder gsonBuilder = new GsonBuilder();
            JsonDeserializer<RunInstances> deserializer = (jsonElement, type, context) -> {
                String id = jsonElement
                        .getAsJsonObject().get("responseElements")
                        .getAsJsonObject().get("instancesSet")
                        .getAsJsonObject().get("items")
                        .getAsJsonArray().get(0).getAsJsonObject().get("instanceId").getAsString();
                String owner = jsonElement.getAsJsonObject().get("userIdentity").getAsJsonObject().get("userName").getAsString();
                return new RunInstances(id, owner);
            };

            gsonBuilder.registerTypeAdapter(RunInstances.class, deserializer);

            Gson gson = gsonBuilder.setLenient().create();
            List<RunInstances> runInstances = gson.fromJson(json, new TypeToken<List<RunInstances>>() {}.getType());
            events.addAll(runInstances);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void filterTaggedResources(Log log) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<Instance> instancesWithoutTags = describeInstances(log);
        for (Instance instancesWithoutTag : instancesWithoutTags) {
            for (Event event : events) {
                String instanceId = instancesWithoutTag.getInstanceId();
                String eventId = event.getId();
                if (instanceId.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        this.events = untaggedEvents;
    }

    private List<Instance> describeInstances(Log log) {
        List<Instance> instances = new ArrayList<>();
        boolean done = false;
        while (!done) {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = ec2.describeInstances(request);

            for (Reservation reservation : response.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    if (instance.getTags().stream().noneMatch(t -> t.getKey().equals(log.getAccount().getOwnerTag()))) {
                        instances.add(instance);
                    }
                }

            }
            request.setNextToken(response.getNextToken());

            if (response.getNextToken() == null) {
                done = true;
            }
        }
        return instances;
    }
}
