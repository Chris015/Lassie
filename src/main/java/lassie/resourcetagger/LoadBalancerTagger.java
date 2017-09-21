package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.awsHandlers.ELBHandler;
import lassie.model.Log;
import lassie.config.Account;
import lassie.model.Event;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadBalancerTagger implements ResourceTagger {
    private final Logger log = Logger.getLogger(LoadBalancerTagger.class);
    private List<Event> events = new ArrayList<>();
    private ELBHandler elbHandler;

    public LoadBalancerTagger(ELBHandler elbHandler) {
        this.elbHandler = elbHandler;
    }

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateClient(log.getAccount());
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateClient(Account account) {
        elbHandler.instantiateELBClient(account.getAccessKeyId(), account.getSecretAccessKey(), account.getRegions().get(0));
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
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
                    log.info("ELB model created. Id: " + id + " Owner: " + owner);
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createLoadBalancerEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createLoadBalancerEvents);
            } catch (IOException e) {
                log.error("Could not parse json: ", e);
                e.printStackTrace();
            }
        }
        log.info("Done parsing json");
    }

    private void filterTaggedResources(String ownerTag) {
        log.info("Filtering tagged LoadBalancers");
        List<Event> untaggedEvents = new ArrayList<>();
        List<LoadBalancer> loadBalancersWithoutTag = elbHandler.describeLoadBalancers(ownerTag);
        for (LoadBalancer loadBalancer : loadBalancersWithoutTag) {
            for (Event event : events) {
                String arn = loadBalancer.getLoadBalancerArn();
                String eventId = event.getId();
                if (arn.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        this.events = untaggedEvents;
        log.info("Done filtering tagged LoadBalancers");
    }

    private void tag(String ownerTag) {
        log.info("Tagging LoadBalancers");
        for (Event event : events) {
            elbHandler.tagResources(event.getId(), ownerTag, event.getOwner());
            log.info("Tagged: " + event.getId() +
                    " with key: " + ownerTag +
                    " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging LoadBalancers");
    }
}
