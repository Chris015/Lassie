package lassie.resourcetagger;

import com.google.gson.*;
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

public class EC2InstanceTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(EC2InstanceTagger.class);

    private EC2Handler ec2Handler;
    private List<Event> events = new ArrayList<>();

    public EC2InstanceTagger(EC2Handler ec2Handler) {
        this.ec2Handler = ec2Handler;
    }

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateEC2Client(log.getAccount());
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateEC2Client(Account account) {
        ec2Handler.instantiateEC2Client(account.getAccessKeyId(), account.getSecretAccessKey(), account.getRegions().get(0));
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
                    log.info("EC2 instance model created. Id: " + id + " Owner: " + owner);
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

        for (Event event : events) {
            if (!ec2Handler.instanceHasTag(event.getId(), ownerTag)) {
                untaggedEvents.add(event);
            }
        }
        this.events = untaggedEvents;
        log.info("Done filtering tagged EC2 instances");
    }

    private void tag(String ownerTag) {
        log.info("Tagging EC2 instances");
        for (Event event : events) {
            ec2Handler.tagResource(event.getId(), ownerTag, event.getOwner());
            log.info("Tagged: " + event.getId() + " with key: " + ownerTag + " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging EC2 instances");
    }
}
