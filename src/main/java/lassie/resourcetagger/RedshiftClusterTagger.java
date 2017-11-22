package lassie.resourcetagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awshandlers.RedshiftHandler;
import lassie.config.Account;
import lassie.model.Event;
import lassie.model.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RedshiftClusterTagger implements ResourceTagger {
    private static final Logger logger = LogManager.getLogger(RedshiftClusterTagger.class);
    private RedshiftHandler redshiftHandler;
    private List<Event> events = new ArrayList<>();

    public RedshiftClusterTagger(RedshiftHandler redshiftHandler) {
        this.redshiftHandler = redshiftHandler;
    }

    @Override
    public void tagResources(Account account) {
        for (Log log : account.getLogs()) {
            ThreadContext.put("region", log.getRegion());
            logger.info("Trying to tag Redshift clusters in region {} for date: {}", log.getRegion(), log.getDate());
            redshiftHandler.instantiateRedshiftClient(account.getAccessKeyId(), account.getSecretAccessKey(), log.getRegion());
            parseJson(account, log.getFilePaths());
            filterEventsWithoutTag(account.getOwnerTag());
            tag(account.getOwnerTag());
        }
    }

    private void parseJson(Account account, List<String> filePaths) {
        logger.trace("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'CreateCluster' && @.responseElements != null)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String clusterId = jsonElement
                            .getAsJsonObject().get("requestParameters")
                            .getAsJsonObject().get("clusterIdentifier")
                            .getAsString();
                    String arn = "arn:aws:redshift:"
                            + account.getRegions().get(0) + ":"
                            + account.getAccountId() + ":cluster:"
                            + clusterId;
                    String owner = jsonElement
                            .getAsJsonObject().get("userIdentity")
                            .getAsJsonObject().get("arn")
                            .getAsString();
                    return new Event(arn, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createClusterEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createClusterEvents);
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
        logger.trace("Filtering RedShift clusters without: {}", ownerTag);
        List<Event> untaggedRedShiftClusters = new ArrayList<>();
        List<String> untaggedRedshiftClusterIds = redshiftHandler.getIdsForUntaggedRedshiftClustersWithoutTag(ownerTag);

        for (Event event : events) {
            if (untaggedRedshiftClusterIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedRedShiftClusters.add(event);
            }
        }
        logger.trace("Done filtering RedShift clusters");
        this.events = untaggedRedShiftClusters;
    }

    private void tag(String ownerTag) {
        logger.info("Tagging RedShift clusters");
        if (events.size() == 0) {
            logger.info("No untagged Redshift clusters found in cloud-trail logs");
        }
        for (Event event : events) {
            redshiftHandler.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        logger.trace("Done tagging RedShift clusters");
    }
}
