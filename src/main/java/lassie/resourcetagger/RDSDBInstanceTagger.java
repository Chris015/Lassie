package lassie.resourcetagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awshandlers.RDSHandler;
import lassie.model.Log;
import lassie.config.Account;
import lassie.model.Event;
import org.apache.log4j.Logger;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RDSDBInstanceTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(RDSDBInstanceTagger.class);
    private List<Event> events = new ArrayList<>();
    private RDSHandler rdsHandler;

    public RDSDBInstanceTagger(RDSHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateRDSClient(log.getAccount());
            parseJson(log.getFilePaths());
            filterEventsWithoutTag(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateRDSClient(Account account) {
        rdsHandler.instantiateRDSClient(account.getAccessKeyId(), account.getSecretAccessKey(), account.getRegions().get(0));
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'CreateDBInstance' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("dBInstanceArn")
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
                List<Event> createDBInstanceEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createDBInstanceEvents);
            } catch (IOException e) {
                log.error("Could not parse json: ", e);
                e.printStackTrace();
            }
        }
        log.info("Done parsing json");
    }

    private void filterEventsWithoutTag(String ownerTag) {
        log.info("Filtering tagged DB instances");
        List<Event> untaggedEvents = new ArrayList<>();
        List<String> untaggedDBInstanceIds = rdsHandler.getIdsForDBInstancesWithoutTag(ownerTag);
        for (Event event : events) {
            if (untaggedDBInstanceIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedEvents.add(event);
            }
        }
        log.info("Done filtering tagged DB instances");
        this.events = untaggedEvents;
    }

    private void tag(String ownerTag) {
        log.info("Tagging DB instances");
        if (events.size() == 0) {
            log.info("No untagged DB instances found in log files");
        }
        for (Event event : events) {
            rdsHandler.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging DB instances");
    }
}
