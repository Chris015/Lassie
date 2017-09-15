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
import lassie.config.Account;
import lassie.event.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EBSVolumeTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(EBSVolumeTagger.class);
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
        String jsonPath = "$..Records[?(@.eventName == 'CreateVolume' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("volumeId")
                            .getAsString();
                    String owner = jsonElement.getAsJsonObject()
                            .get("userIdentity")
                            .getAsJsonObject()
                            .get("arn").getAsString();
                    log.info("EBS volume event created. Id: " + id + " Owner: " + owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createVolumeEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {}.getType());
                events.addAll(createVolumeEvents);
            } catch (IOException e) {
                log.error("Could nog parse json: ", e);
                e.printStackTrace();
            }
        }
        log.info("Done parsing json");
    }

    private void filterTaggedResources(String ownerTag) {
        log.info("Filtering tagged EBS volume");
        List<Event> untaggedEvents = new ArrayList<>();
        List<Volume> volumesWithoutTags = describeVolumes(ownerTag);
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
        log.info("Done filtering tagged EBS volumes");
    }

    private List<Volume> describeVolumes(String ownerTag) {
        log.info("Describing volumes");
        List<Volume> volumesWithoutTags = new ArrayList<>();
        boolean done = false;
        while (!done) {
            DescribeVolumesRequest request = new DescribeVolumesRequest();
            DescribeVolumesResult result = ec2.describeVolumes(request);
            for (Volume volume : result.getVolumes()) {
                if (!hasTag(volume, ownerTag)) {
                    volumesWithoutTags.add(volume);
                }
            }
            request.setNextToken(result.getNextToken());
            if (result.getNextToken() == null) {
                done = true;
            }
        }
        log.info("Found " + volumesWithoutTags.size() + " EBS volumes without " + ownerTag);
        return volumesWithoutTags;
    }

    private boolean hasTag(Volume volume, String tag) {
        log.trace(tag + " found: " + volume.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return volume.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }

    private void tag(String ownerTag) {
        log.info("Tagging volumes");
        for (Event event : events) {
            CreateTagsRequest tagsRequest = new CreateTagsRequest()
                    .withResources(event.getId())
                    .withTags(new Tag(ownerTag, event.getOwner()));
            ec2.createTags(tagsRequest);
            log.info("Tagged: " + event.getId()
                    + " with key: " + ownerTag
                    + " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Tagging volumes complete");
    }
}
