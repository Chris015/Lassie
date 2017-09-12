package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.*;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TagDescription;
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
            filterTaggedResources(log);
            tag(log);
        }
    }

    private void instantiateClient(Log log) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(log.getAccount().getAccessKeyId(), log.getAccount().getSecretAccessKey());
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.elb = AmazonElasticLoadBalancingClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(log.getAccount().getRegions().get(0))
                .build();
    }

    private void parseJson(List<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                String json = JsonPath.parse(new File(filePath))
                        .read("$..Records[?(@.eventName == 'CreateLoadBalancer' && @.responseElements != null)]")
                        .toString();
                GsonBuilder gsonBuilder = new GsonBuilder();
                JsonDeserializer<Event> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("loadBalancers")
                            .getAsJsonArray().get(0).getAsJsonObject().get("loadBalancerArn")
                            .getAsString();
                    String owner = jsonElement.getAsJsonObject().get("userIdentity").getAsJsonObject().get("arn").getAsString();
                    return new Event(id, owner);
                };

                gsonBuilder.registerTypeAdapter(Event.class, deserializer);

                Gson gson = gsonBuilder.setLenient().create();
                List<Event> createLoadBalancers = gson.fromJson(
                        json, new TypeToken<List<Event>>() {
                        }.getType());
                events.addAll(createLoadBalancers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<LoadBalancer> describeLoadBalancers(Log log) {
        List<LoadBalancer> loadBalancers = new ArrayList<>();
        DescribeLoadBalancersResult result = elb.describeLoadBalancers(new DescribeLoadBalancersRequest());

        for (LoadBalancer loadBalancer : result.getLoadBalancers()) {
            DescribeTagsRequest tagsRequest = new DescribeTagsRequest().withResourceArns(loadBalancer.getLoadBalancerArn());
            DescribeTagsResult tagsResult = elb.describeTags(tagsRequest);

            for (TagDescription tagDescription : tagsResult.getTagDescriptions()) {
                if (tagDescription.getTags().isEmpty() || tagDescription.getTags().stream().noneMatch(t -> t.getKey().equals(log.getAccount().getOwnerTag()))) {
                    loadBalancers.add(loadBalancer);

                }
            }

        }
        return loadBalancers;
    }

    private void filterTaggedResources(Log log) {
        List<Event> untaggedEvents = new ArrayList<>();
        List<LoadBalancer> loadBalancersWithoutTags = describeLoadBalancers(log);

        for (LoadBalancer loadBalancer : loadBalancersWithoutTags) {
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

    private void tag(Log log) {
        for (Event event : events) {
            com.amazonaws.services.elasticloadbalancingv2.model.Tag tag = new com.amazonaws.services.elasticloadbalancingv2.model.Tag();
            tag.setKey(log.getAccount().getOwnerTag());
            tag.setValue(event.getOwner());
            AddTagsRequest tagsRequest = new AddTagsRequest()
                    .withResourceArns(event.getId())
                    .withTags(tag);
            elb.addTags(tagsRequest);
            System.out.println("Tagged: " + event.getId() +
                    " with key: " + log.getAccount().getOwnerTag() +
                    " value: " + event.getOwner());
        }
    }

}
