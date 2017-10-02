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

public class SecurityGroupTagger implements ResourceTagger {
    private final static Logger log = Logger.getLogger(SecurityGroupTagger.class);
    private List<Event> events = new ArrayList<>();
    private Ec2Handler ec2Handler;

    public SecurityGroupTagger(Ec2Handler ec2Handler) {
        this.ec2Handler = ec2Handler;
    }

    @Override
    public void tagResources(Account account) {
        instantiateEc2Client(account);
        for (Log log : account.getLogs()) {
            parseJson(log.getFilePaths());
            filterEventsWithoutTag(account.getOwnerTag());
            tag(account.getOwnerTag());
        }
    }

    private void instantiateEc2Client(Account account) {
        ec2Handler.instantiateEC2Client(account.getAccessKeyId(), account.getSecretAccessKey(), account.getRegions().get(0));
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'CreateSecurityGroup' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("groupId")
                            .getAsString();
                    String owner = jsonElement
                            .getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    log.info("Event created with Id: " + id + " Owner: " + owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createSecurityGroupEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createSecurityGroupEvents);
            } catch (IOException e) {
                log.error("Could not parse json: ", e);
                e.printStackTrace();
            }
        }
        log.info("Done parsing json");
    }

    private void filterEventsWithoutTag(String ownerTag) {
        log.info("Filtering tagged Security groups");
        List<Event> untaggedSecurityGroups = new ArrayList<>();
        List<String> untaggedSecurityGroupIds = ec2Handler.getIdsForSecurityGroupsWithoutTag(ownerTag);

        for (Event event : events) {
            if (untaggedSecurityGroupIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedSecurityGroups.add(event);
            }
        }

        this.events = untaggedSecurityGroups;
        log.info("Done filtering tagged Security groups");
    }

    private void tag(String ownerTag) {
        log.info("Tagging Security groups");
        if (events.size() == 0) {
            log.info("No untagged Security groups found in log files");
        }
        for (Event event : events) {
            ec2Handler.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging Security groups");
    }
}
