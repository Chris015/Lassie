package lassie.resourcetagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awshandlers.Ec2Handler;
import lassie.config.Account;
import lassie.model.Event;
import lassie.model.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EBSVolumeTagger implements ResourceTagger {
    private static final Logger logger = LogManager.getLogger(EBSVolumeTagger.class);
    private Ec2Handler ec2Handler;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(Account account) {
        instantiateEc2Client(account);
        for (Log log : account.getLogs()) {
            parseJson(log.getFilePaths());
            filterEventsWithoutTag(account.getOwnerTag());
            tag(account.getOwnerTag());
        }
    }

    public EBSVolumeTagger(Ec2Handler ec2Handler) {
        this.ec2Handler = ec2Handler;
    }

    private void instantiateEc2Client(Account account) {
        ec2Handler.instantiateEC2Client(account.getAccessKeyId(), account.getSecretAccessKey(), account.getRegions().get(0));
    }

    private void parseJson(List<String> filePaths) {
        logger.info("Parsing json");
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
                    logger.info("Event created with Id: {} Owner: {}" , id, owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createVolumeEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createVolumeEvents);
            } catch (IOException e) {
                logger.error("Could nog parse json: ", e);
                e.printStackTrace();
            }
        }
        logger.info("Done parsing json");
    }

    private void filterEventsWithoutTag(String ownerTag) {
        logger.info("Filtering EBS volumes without: {}", ownerTag);
        List<Event> untaggedVolumes = new ArrayList<>();

        List<String> untaggedVolumeIds = ec2Handler.getIdsForVolumesWithoutTag(ownerTag);

        for (Event event : events) {
            if (untaggedVolumeIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedVolumes.add(event);
            }
        }

        List<String> volumesWithoutEvents = new ArrayList<>();
        volumesWithoutEvents.addAll(getIdsForUntaggedVolumesWithoutEvents(untaggedVolumeIds));
        try {
            for (String id : volumesWithoutEvents) {
                logger.info("Can't find {} in the log files. Checking if it's attached to an instance", id);
                if (ec2Handler.volumeIsAttachedToInstance(id)) {
                    String instanceId = ec2Handler.getIdForInstanceVolumeIsAttachedTo(id);
                    logger.info("Volume {} is attached to instance: {}. Trying to fetch the {} of the instance",
                            id,
                            instanceId,
                            ownerTag);

                    String instanceOwner = ec2Handler.getTagValueForInstanceWithId(ownerTag, instanceId);
                    untaggedVolumes.add(new Event(id, instanceOwner));
                    logger.info("Found {}. Prepared volume for tagging", ownerTag);
                }
            }
        } catch (IllegalArgumentException e) {
            logger.warn(e.getMessage() + ". Tag the instance before you try to tag the EBSVolume");
        }

        this.events = untaggedVolumes;
        logger.info("Done filtering EBS volumes");
    }

    private List<String> getIdsForUntaggedVolumesWithoutEvents(List<String> untaggedVolumeIds) {
        List<String> volumesWithoutEvents = new ArrayList<>();

        for (String untaggedVolumeId : untaggedVolumeIds) {
            if (events.stream().noneMatch(event -> event.getId().equals(untaggedVolumeId))) {
                volumesWithoutEvents.add(untaggedVolumeId);
            }
        }
        return volumesWithoutEvents;
    }

    private void tag(String ownerTag) {
        logger.info("Tagging volumes");
        if (events.size() == 0) {
            logger.info("No untagged Volumes found in log files");
        }
        for (Event event : events) {
            ec2Handler.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        logger.info("Tagging volumes complete");
    }
}
