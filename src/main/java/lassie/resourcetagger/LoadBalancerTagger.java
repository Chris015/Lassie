package lassie.resourcetagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awshandlers.ELBHandler;
import lassie.config.Account;
import lassie.model.Event;
import lassie.model.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadBalancerTagger implements ResourceTagger {
    private static final Logger logger = LogManager.getLogger(LoadBalancerTagger.class);
    private List<Event> events = new ArrayList<>();
    private ELBHandler elbHandlerImpl;

    public LoadBalancerTagger(ELBHandler elbHandlerImpl) {
        this.elbHandlerImpl = elbHandlerImpl;
    }

    @Override
    public void tagResources(Account account) {
        for (Log log : account.getLogs()) {
            elbHandlerImpl.instantiateELBClient(account.getAccessKeyId(), account.getSecretAccessKey(), log.getRegion());
            parseJson(log.getFilePaths());
            filterEventsWithoutTag(account.getOwnerTag());
            tag(account.getOwnerTag());
        }
    }

    private void parseJson(List<String> filePaths) {
        logger.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'CreateLoadBalancer' && @.responseElements.loadBalancers)]";
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath)).read(jsonPath).toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("loadBalancers")
                            .getAsJsonArray().get(0)
                            .getAsJsonObject().get("loadBalancerArn")
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
                List<Event> createLoadBalancerEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createLoadBalancerEvents);
            } catch (IOException e) {
                logger.error("Could not parse json: ", e);
                e.printStackTrace();
            }
        }
        logger.info("Done parsing json");
    }

    private void filterEventsWithoutTag(String ownerTag) {
        logger.info("Filtering tagged LoadBalancers without: {}", ownerTag);
        List<Event> untaggedLoadBalancers = new ArrayList<>();
        List<String> untaggedLoadBalancerIds = elbHandlerImpl.getIdsForLoadBalancersWithoutTag(ownerTag);
        for (Event event : events) {
            if (untaggedLoadBalancerIds.stream().anyMatch(id -> id.equals(event.getId()))) {
                untaggedLoadBalancers.add(event);
            }
        }
        this.events = untaggedLoadBalancers;
        logger.info("Done filtering tagged LoadBalancers");
    }

    private void tag(String ownerTag) {
        logger.info("Tagging LoadBalancers");
        if (events.size() == 0) {
            logger.info("No untagged LoadBalancers found in log files");
        }
        for (Event event : events) {
            elbHandlerImpl.tagResource(event.getId(), ownerTag, event.getOwner());
        }
        this.events = new ArrayList<>();
        logger.info("Done tagging LoadBalancers");
    }
}
