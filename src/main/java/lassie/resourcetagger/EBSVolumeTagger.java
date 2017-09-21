package lassie.resourcetagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awshandlers.EC2Handler;
import lassie.model.Log;
import lassie.config.Account;
import lassie.model.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EBSVolumeTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(EBSVolumeTagger.class);
    private EC2Handler ec2Handler;
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

    public EBSVolumeTagger(EC2Handler ec2Handler) {
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
                    log.info("EBS volume model created. Id: " + id + " Owner: " + owner);
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
        List<Event> untaggedVolumes = new ArrayList<>();
        for (Event event : events) {
            if (!ec2Handler.volumeHasTag(event.getId(), ownerTag)) {
                untaggedVolumes.add(event);
            }
        }
        this.events = untaggedVolumes;
        log.info("Done filtering tagged EBS volumes");
    }

    private void tag(String ownerTag) {
        log.info("Tagging volumes");
        for (Event event : events) {
            ec2Handler.tagResource(event.getId(), ownerTag, event.getOwner());
            log.info("Tagged: " + event.getId() + " with key: " + ownerTag + " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Tagging volumes complete");
    }
}
