package lassie.resourcetagger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import lassie.Log;
import lassie.event.CreateLoadBalancer;
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
                JsonDeserializer<CreateLoadBalancer> deserializer = (jsonElement, type, context) -> {
                    String id = jsonElement
                            .getAsJsonObject().get("responseElements")
                            .getAsJsonObject().get("loadBalancers")
                            .getAsJsonArray().get(0).getAsJsonObject().get("loadBalancerArn")
                            .getAsString();
                    String owner = jsonElement.getAsJsonObject().get("userIdentity").getAsJsonObject().get("arn").getAsString();

                    return new CreateLoadBalancer(id, owner);
                };

                gsonBuilder.registerTypeAdapter(CreateLoadBalancer.class, deserializer);

                Gson gson = gsonBuilder.setLenient().create();
                List<CreateLoadBalancer> createLoadBalancers = gson.fromJson(
                        json, new TypeToken<List<CreateLoadBalancer>>() {
                        }.getType());
                events.addAll(createLoadBalancers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
