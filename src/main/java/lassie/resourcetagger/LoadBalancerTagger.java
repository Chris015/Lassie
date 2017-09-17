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
    private AmazonElasticLoadBalancing elb;
    private List<Event> events = new ArrayList<>();

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
        log.info("Instantiating ELB client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(account.getAccessKeyId(),
                account.getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.elb = AmazonElasticLoadBalancingClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(account.getRegions().get(0))
                .build();
        log.info("ELB client instantiated");
    }

    private void parseJson(List<String> filePaths) {
        log.info("Parsing json");
        String jsonPath = "$..Records[?(@.eventName == 'CreateLoadBalancer' && @.responseElements != null)]";
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
            log.info("Done parsing json");
        }
    }

    private void filterTaggedResources(String ownerTag) {
        log.info("Filtering tagged LoadBalancers");
        List<Event> untaggedEvents = new ArrayList<>();
        List<LoadBalancer> loadBalancersWithoutTag = describeLoadBalancers(ownerTag);
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

    private List<LoadBalancer> describeLoadBalancers(String ownerTag) {
        log.info("Describing Load Balancers");
        List<LoadBalancer> loadBalancers = new ArrayList<>();
        DescribeLoadBalancersResult result = elb.describeLoadBalancers(new DescribeLoadBalancersRequest());
        for (LoadBalancer loadBalancer : result.getLoadBalancers()) {
            DescribeTagsRequest tagsRequest = new DescribeTagsRequest()
                    .withResourceArns(loadBalancer.getLoadBalancerArn());
            DescribeTagsResult tagsResult = elb.describeTags(tagsRequest);
            for (TagDescription tagDescription : tagsResult.getTagDescriptions()) {
                if (!hasTag(tagDescription, ownerTag)) {
                    loadBalancers.add(loadBalancer);
                }
            }
        }
        log.info("Found " + loadBalancers.size() + " LoadBalancers without " + ownerTag);
        return loadBalancers;
    }

    private boolean hasTag(TagDescription tagDescription, String tag) {
        log.trace(tag + " found: " + tagDescription.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return tagDescription.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }

    private void tag(String ownerTag) {
        log.info("Tagging LoadBalancers");
        for (Event event : events) {
            Tag tag = new Tag();
            tag.setKey(ownerTag);
            tag.setValue(event.getOwner());
            AddTagsRequest tagsRequest = new AddTagsRequest()
                    .withResourceArns(event.getId())
                    .withTags(tag);
            elb.addTags(tagsRequest);
            log.info("Tagged: " + event.getId() +
                    " with key: " + ownerTag +
                    " value: " + event.getOwner());
        }
        this.events = new ArrayList<>();
        log.info("Done tagging LoadBalancers");
    }
}
