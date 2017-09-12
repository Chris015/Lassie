package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
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

public class EBSVolumeTagger implements ResourceTagger {
    private AmazonEC2 ec2;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        System.out.println("Parsing logs and tagging resources...");
        for (Log log : logs) {
            instantiateEc2Client(log);
            parseJson(log.getFilePaths());
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

    private void parseJson(List<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath))
                        .read("$..Records[?(@.eventName == 'CreateVolume' && @.responseElements != null)]")
                        .toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("volumeId").getAsString();
                    String owner = jsonElement.getAsJsonObject().get("userIdentity").getAsJsonObject().get("arn").getAsString();
                    return new Event(id, owner);
                };

                gsonBuilder.registerTypeAdapter(Event.class, deserializer);

                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createSecurityGroups = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createSecurityGroups);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void filterTaggedResources(Log log) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<Volume> volumesWithoutTags = describeVolumes(log);
        for (Volume volumesWithoutTag : volumesWithoutTags) {
            for (Event event : events) {
                String volumeId = volumesWithoutTag.getVolumeId();
                String eventId = event.getId();
                if (volumeId.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        this.events = untaggedEvents;
    }

    private List<Volume> describeVolumes(Log log) {
        List<Volume> volumes = new ArrayList<>();
        boolean done = false;
        while (!done) {
            DescribeVolumesRequest request = new DescribeVolumesRequest();
            DescribeVolumesResult result = ec2.describeVolumes(request);
            for (Volume volume : result.getVolumes()) {
                if (volume.getTags().stream().noneMatch(t -> t.getKey().equals(log.getAccount().getOwnerTag()))) {
                    volumes.add(volume);
                }
            }
            request.setNextToken(result.getNextToken());

            if (result.getNextToken() == null) {
                done = true;
            }
        }
        return volumes;
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
}
