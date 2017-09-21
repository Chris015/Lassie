package lassie.awsHandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ELBHandler {
    private static final Logger log = Logger.getLogger(ELBHandler.class);
    private AmazonElasticLoadBalancing elb;

    public void instantiateELBClient(String accessKeyId, String secretAccessKey, String region) {
        log.info("Instantiating ELB client");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.elb = AmazonElasticLoadBalancingClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
        log.info("ELB client instantiated");
    }

    public List<LoadBalancer> describeLoadBalancers(String ownerTag) {
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
        loadBalancers.forEach(loadBalancer -> log.info(loadBalancer.getLoadBalancerName()));
        return loadBalancers;
    }

    private boolean hasTag(TagDescription tagDescription, String tag) {
        log.trace(tag + " found: " + tagDescription.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return tagDescription.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }

    public void tagResources(String id, String ownerTag, String owner) {
        Tag tag = new Tag();
        tag.setKey(ownerTag);
        tag.setValue(owner);
        AddTagsRequest tagsRequest = new AddTagsRequest()
                .withResourceArns(id)
                .withTags(tag);
        elb.addTags(tagsRequest);
    }
}
