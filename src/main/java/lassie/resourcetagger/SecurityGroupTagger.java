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

public class SecurityGroupTagger implements ResourceTagger {
    private static final Logger logger = LogManager.getLogger(SecurityGroupTagger.class);
    private List<Event> events = new ArrayList<>();
    private Ec2Handler ec2Handler;

    public SecurityGroupTagger(Ec2Handler ec2Handler) {
        this.ec2Handler = ec2Handler;
    }

    @Override
    public void tagResources(Account account) {
        for (Log log : account.getLogs()) {
            logger.info("Trying to tag Security groups in region {} for date: {}", log.getRegion(), log.getDate());
            ec2Handler.instantiateEC2Client(account.getAccessKeyId(), account.getSecretAccessKey(), log.getRegion());
            parseJson(log.getFilePaths());
            filterEventsWithoutTag(account.getOwnerTag());
            tag(account.getOwnerTag());
        }
    }

    private void parseJson(List<String> filePaths) {
        logger.trace("Parsing json");
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
                    logger.info("Event created with Id: {} Owner: {}", id, owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createSecurityGroupEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createSecurityGroupEvents);
            } catch (IOException e) {
                logger.error("Could not parse json: {} \nError: {}", filePath, e);
                e.printStackTrace();
            }
        }
        logger.info("Found: {} events in cloud-trail logs", events.size());
        events.forEach(event -> logger.info("Id: {} Owner: {}", event.getId(), event.getOwner()));
        logger.trace("Done parsing json");
    }

    private void filterEventsWithoutTag(String ownerTag) {
        logger.trace("Filtering Security groups without: {}", ownerTag);
        List<Event> untaggedSecurityGroups = new ArrayList<>();
        List<String> untaggedSecurityGroupIds = ec2Handler.getIdsForSecurityGroupsWithoutTag(ownerTag);

        for (Event event : events) {
            if (untaggedSecurityGroupIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedSecurityGroups.add(event);
            }
        }

        this.events = untaggedSecurityGroups;
        logger.trace("Done filtering Security groups");
    }

    private void tag(String ownerTag) {
        logger.trace("Tagging Security groups");
        if (events.size() == 0) {
            logger.info("No untagged Security groups found in cloud-trail logs");
        }
        for (Event event : events) {
            ec2Handler.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        logger.trace("Done tagging Security groups");
    }
}
