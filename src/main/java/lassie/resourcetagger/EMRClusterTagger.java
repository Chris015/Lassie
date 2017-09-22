package lassie.resourcetagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awshandlers.EMRHandler;
import lassie.model.Log;
import lassie.config.Account;
import lassie.model.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EMRClusterTagger implements ResourceTagger {
    private final static Logger log = Logger.getLogger(EMRClusterTagger.class);
    private EMRHandler emrHandler;
    private List<Event> events = new ArrayList<>();

    public EMRClusterTagger(EMRHandler emrHandler) {
        this.emrHandler = emrHandler;
    }

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateEmrInstance(log.getAccount());
            parseJson(log.getFilePaths());
            filterEventsWithoutTag(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateEmrInstance(Account account) {
        log.info("Instantiating EMR client");
        emrHandler.instantiateEMRClient(account.getAccessKeyId(), account.getSecretAccessKey(), account.getRegions().get(0));
        log.info("EMR client instantiated");
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
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
        log.info("Filtering EMR-clusters without: " + ownerTag);
        List<Event> untaggedClusters = new ArrayList<>();

        List<String> untaggedClusterIds = emrHandler.getIdsForClustersWithoutTag(ownerTag);
        for (Event event : events) {
            if(untaggedClusterIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedClusters.add(event);
            }
        }

        this.events = untaggedClusters;
        log.info("Done filtering EMR-clusters");
    }

    private void tag(String ownerTag) {
        log.info("Tagging EMR clusters");
        for (Event event : events) {
            emrHandler.tagResource(event.getId(), ownerTag, event.getOwner());
            log.info("Tagged: " + event.getId()
                    + " with key: " + ownerTag
                    + " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging EMR clusters");
    }
}

