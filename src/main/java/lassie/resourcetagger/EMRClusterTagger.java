package lassie.resourcetagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awshandlers.EMRHandler;
import lassie.config.Account;
import lassie.model.Event;
import lassie.model.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EMRClusterTagger implements ResourceTagger {
    private static final Logger logger = LogManager.getLogger(EMRClusterTagger.class);
    private EMRHandler emrHandler;
    private List<Event> events = new ArrayList<>();

    public EMRClusterTagger(EMRHandler emrHandler) {
        this.emrHandler = emrHandler;
    }

    @Override
    public void tagResources(Account account) {
        for (Log log : account.getLogs()) {
            logger.info("Trying to tag ELB in region {} for date: {}", log.getRegion(), log.getDate());
            emrHandler.instantiateEMRClient(account.getAccessKeyId(), account.getSecretAccessKey(), log.getRegion());
            parseJson(log.getFilePaths());
            filterEventsWithoutTag(account.getOwnerTag());
            tag(account.getOwnerTag());
        }
    }

    private void parseJson(List<String> filePaths) {
        logger.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'RunJobFlow' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath))
                        .read(jsonPath)
                        .toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("jobFlowId")
                            .getAsString();
                    String owner = jsonElement.getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createDBInstanceEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createDBInstanceEvents);
            } catch (IOException e) {
                logger.error("Could not parse json: ", e);
                e.printStackTrace();
            }
        }
        logger.info("Found: {} events in cloud-trail logs", events.size());
        events.forEach(event -> logger.info("Id: {} Owner: {}", event.getId(), event.getOwner()));
        logger.trace("Done parsing json");
    }

    private void filterEventsWithoutTag(String ownerTag) {
        logger.trace("Filtering EMR-clusters without: {}", ownerTag);
        List<Event> untaggedClusters = new ArrayList<>();

        List<String> untaggedClusterIds = emrHandler.getIdsForClustersWithoutTag(ownerTag);
        for (Event event : events) {
            if (untaggedClusterIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedClusters.add(event);
            }
        }

        this.events = untaggedClusters;
        logger.trace("Done filtering EMR-clusters");
    }

    private void tag(String ownerTag) {
        logger.trace("Tagging EMR clusters");
        if (events.size() == 0) {
            logger.info("No untagged LoadBalancers found in cloud-trail logs");
        }
        for (Event event : events) {
            emrHandler.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        logger.trace("Done tagging EMR clusters");
    }
}

