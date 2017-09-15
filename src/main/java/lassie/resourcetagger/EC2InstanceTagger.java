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
import lassie.config.Account;
import lassie.event.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EC2InstanceTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(EC2InstanceTagger.class);

    private AmazonEC2 ec2;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateEc2Client(log.getAccount());
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateEc2Client(Account account) {
        log.info("Instantiating EC2 client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(account.getAccessKeyId(), account.getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(account.getRegions().get(0))
                .build();
        log.info("EC2 client instantiated");
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'RunInstances' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("instancesSet")
                            .getAsJsonObject().get("items")
                            .getAsJsonArray().get(0).getAsJsonObject().get("instanceId")
                            .getAsString();
                    String owner = jsonElement
                            .getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    log.info("EC2 instance event created. Id: " + id + " Owner: " + owner);
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
        log.info("Filtering tagged EC2 instances");
        List<Event> untaggedEvents = new ArrayList<>();
        List<Instance> instancesWithoutTags = describeInstances(ownerTag);
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
        log.info("Done filtering tagged EC2 instances");

    }

    private List<Instance> describeInstances(String ownerTag) {
        log.info("Describing Instances");
        List<Instance> instances = new ArrayList<>();
        boolean done = false;
        while (!done) {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = ec2.describeInstances(request);
            for (Reservation reservation : response.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    if (!hasTag(instance, ownerTag)) {
                        instances.add(instance);
                    }
                }
            }
            request.setNextToken(response.getNextToken());
            if (response.getNextToken() == null) {
                done = true;
            }
        }
        log.info("Found " + instances.size() + " instances without " + ownerTag);
        return instances;
    }

    private boolean hasTag(Instance instance, String tag) {
        log.trace(tag + " found: " + instance.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return instance.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }

    private void tag(String ownerTag) {
        log.info("Tagging EC2 instances");
        for (Event event : events) {
            CreateTagsRequest tagsRequest = new CreateTagsRequest()
                    .withResources(event.getId())
                    .withTags(new Tag(ownerTag, event.getOwner()));
            ec2.createTags(tagsRequest);
            log.info("Tagged: " + event.getId() +
                    " with key: " + ownerTag +
                    " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging EC2 instances");
    }
}
