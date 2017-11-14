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
    private static final  int MAX_RETRIES = 3;
    private int secondsToSleep = 5;
    private int retries = 0;

    public EMRClusterTagger(EMRHandler emrHandler) {
        this.emrHandler = emrHandler;
    }

    @Override
    public void tagResources(Account account) {
        for (Log log : account.getLogs()) {
            logger.info("Trying to tag EMR clusters in region {} for date: {}", log.getRegion(), log.getDate());
            emrHandler.instantiateEMRClient(account.getAccessKeyId(), account.getSecretAccessKey(), log.getRegion());
            parseJson(log.getFilePaths());
            boolean success = filterEventsWithoutTag(account.getOwnerTag());
            if (success) {
                tag(account.getOwnerTag());
            } else {
                events = new ArrayList<>();
                logger.error("Failed to list EMR clusters. EMR clusters will not be tagged");
            }
        }
    }

    private void parseJson(List<String> filePaths) {
        logger.trace("Parsing json");
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
                logger.error("Could not parse json: {} \nError: {}", filePath, e);
                e.printStackTrace();
            }
        }
        logger.info("Found: {} events in cloud-trail logs", events.size());
        events.forEach(event -> logger.info("Id: {} Owner: {}", event.getId(), event.getOwner()));
        logger.trace("Done parsing json");
    }

    private boolean filterEventsWithoutTag(String ownerTag) {
        logger.trace("Filtering EMR clusters without: {}", ownerTag);
        List<Event> untaggedClusters = new ArrayList<>();
        List<String> untaggedClusterIds = null;

        while (retries < MAX_RETRIES) {
            logger.info("retries {}", retries);
            try {
                if (untaggedClusterIds != null) break;
                untaggedClusterIds = emrHandler.getIdsForClustersWithoutTag(ownerTag);
            } catch (Exception e) {
                logger.error("There was an issue while listing EMR clusters. The application will sleep for {} seconds\n"
                        + "and try again\nError: {}", secondsToSleep, e);
                try {
                    Thread.sleep(secondsToSleep * 1_000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                secondsToSleep += secondsToSleep;
                retries++;
            }
        }

        secondsToSleep = 5;
        retries = 0;

        if (untaggedClusterIds == null) {
            return false;
        }

        for (Event event : events) {
            if (untaggedClusterIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedClusters.add(event);
            }
        }

        this.events = untaggedClusters;
        logger.trace("Done filtering EMR clusters");
        return true;
    }

    private void tag(String ownerTag) {
        logger.trace("Tagging EMR clusters");
        if (events.size() == 0) {
            logger.info("No untagged EMR clusters found in cloud-trail logs");
        }
        for (Event event : events) {
            emrHandler.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        logger.trace("Done tagging EMR clusters");
    }
}

