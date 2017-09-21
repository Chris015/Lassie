package lassie.resourcetagger;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awsHandlers.RDSHandler;
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
    private AmazonRDS rds;
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
            filterTaggedResources(log.getAccount().getOwnerTag());
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
                    log.info("RDS DB instance model created. Id: " + id + " Owner: " + owner);
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

    private void filterTaggedResources(String ownerTag) {
        log.info("Filtering tagged DB instances");
        List<Event> untaggedEvents = new ArrayList<>();
        List<DBInstance> dbInstancesWithoutTag = rdsHandler.describeRDSInstances(ownerTag);
        for (DBInstance dbInstance : dbInstancesWithoutTag) {
            for (Event event : events) {
                String dbId = dbInstance.getDBInstanceArn();
                String eventId = event.getId();
                if (dbId.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        log.info("Done filtering tagged DB instances");
        this.events = untaggedEvents;
    }

    private void tag(String ownerTag) {
        log.info("Tagging DB instances");
        for (Event event : events) {
        rdsHandler.tagResource(event.getId(), ownerTag, event.getOwner());
            log.info("Tagged: " + event.getId() + " with key: " + ownerTag + " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging DB instances");
    }
}
