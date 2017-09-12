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
import lassie.Log;
import lassie.event.Event;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadBalancerTagger implements ResourceTagger {
    private AmazonElasticLoadBalancing elb;
    private List<Event> events = new ArrayList<>();

    @Override
    public void tagResources(List<Log> logs) {
        for (Log log : logs) {
            instantiateClient(log);
            parseJson(log.getFilePaths());
            filterTaggedResources(log.getAccount().getOwnerTag());
            tag(log.getAccount().getOwnerTag());
        }
    }

    private void instantiateClient(Log log) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(log.getAccount().getAccessKeyId(),
                log.getAccount().getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.elb = AmazonElasticLoadBalancingClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(log.getAccount().getRegions().get(0))
                .build();
    }

    private void parseJson(List<String> filePaths) {
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
                    return new Event(id, owner);
                };
                gsonBuilder.registerTypeAdapter(Event.class, deserializer);
                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createLoadBalancerEvents = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createLoadBalancerEvents);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<LoadBalancer> describeLoadBalancers(String ownerTag) {
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
        return loadBalancers;
    }

    private boolean hasTag(TagDescription tagDescription, String ownerTag) {
        return tagDescription.getTags().isEmpty() ||
                tagDescription.getTags().stream().noneMatch(t -> t.getKey().equals(ownerTag));
    }

    private void filterTaggedResources(String ownerTag) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<LoadBalancer> loadBalancersWithoutOwnerTags = describeLoadBalancers(ownerTag);
        for (LoadBalancer loadBalancer : loadBalancersWithoutOwnerTags) {
            for (Event event : events) {
                String arn = loadBalancer.getLoadBalancerArn();
                String eventId = event.getId();
                if (arn.equals(eventId)) {
                    untaggedEvents.add(event);
                }
            }
        }
        this.events = untaggedEvents;
    }

    private void tag(String ownerTag) {
        for (Event event : events) {
            Tag tag = new Tag();
            tag.setKey(ownerTag);
            tag.setValue(event.getOwner());
            AddTagsRequest tagsRequest = new AddTagsRequest()
                    .withResourceArns(event.getId())
                    .withTags(tag);
            elb.addTags(tagsRequest);
            System.out.println("Tagged: " + event.getId() +
                    " with key: " + ownerTag +
                    " value: " + event.getOwner());
        }
    }

}
