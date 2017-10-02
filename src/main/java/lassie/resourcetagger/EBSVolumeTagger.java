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
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EBSVolumeTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(EBSVolumeTagger.class);
    private Ec2Handler ec2Handler;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(Log log) {
        instantiateEc2Client(log.getAccount());
        parseJson(log.getFilePaths());
        filterEventsWithoutTag(log.getAccount().getOwnerTag());
        tag(log.getAccount().getOwnerTag());
    }

    public EBSVolumeTagger(Ec2Handler ec2Handler) {
        this.ec2Handler = ec2Handler;
    }

    private void instantiateEc2Client(Account account) {
        ec2Handler.instantiateEC2Client(account.getAccessKeyId(), account.getSecretAccessKey(), account.getRegions().get(0));
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
                    log.info("Event created with Id: " + id + " Owner: " + owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createVolumeEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createVolumeEvents);
            } catch (IOException e) {
                log.error("Could nog parse json: ", e);
                e.printStackTrace();
            }
        }
        log.info("Done parsing json");
    }

    private void filterEventsWithoutTag(String ownerTag) {
        log.info("Filtering tagged EBS volumes");
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
                log.info("Can't find " + id + " in the log files. Checking if it's attached to an instance");
                if (ec2Handler.volumeIsAttachedToInstance(id)) {
                    String instanceId = ec2Handler.getIdForInstanceVolumeIsAttachedTo(id);
                    log.info("Volume " + id + " is attached to instance: " + instanceId + ". Trying to fetch the " + ownerTag + " of the instance");

                    String instanceOwner = ec2Handler.getTagValueForInstanceWithId(ownerTag, instanceId);
                    untaggedVolumes.add(new Event(id, instanceOwner));
                    log.info("Found " + ownerTag + ". Prepared volume for tagging");
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage() + ". Tag the instance before you try to tag the EBSVolume");
        }

        this.events = untaggedVolumes;
        log.info("Done filtering tagged EBS volumes");
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
        log.info("Tagging volumes");
        if (events.size() == 0) {
            log.info("No untagged Volumes found in log files");
        }
        for (Event event : events) {
            ec2Handler.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Tagging volumes complete");
    }
}
