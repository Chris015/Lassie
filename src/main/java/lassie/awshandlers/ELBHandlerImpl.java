package lassie.awshandlers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static lassie.Application.DRY_RUN;

public class ELBHandlerImpl implements ELBHandler {
    private static final Logger logger = LogManager.getLogger(ELBHandlerImpl.class);
    private AmazonElasticLoadBalancing elb;

    public void instantiateELBClient(String accessKeyId, String secretAccessKey, String region) {
        logger.trace("Instantiating ELB client in region: {}", region);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretAccessKey);
        AWSStaticCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(awsCreds);
        this.elb = AmazonElasticLoadBalancingClientBuilder.standard()
                .withCredentials(awsCredentials)
                .withRegion(region)
                .build();
        logger.trace("ELB client instantiated");
    }

    public void tagResource(String id, String key, String value) {
        if (DRY_RUN) {
            logger.info("Dry run: {}. Did not tag: {} with {}: {}", DRY_RUN, id, key, value);
            return;
        }
        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);
        AddTagsRequest tagsRequest = new AddTagsRequest()
                .withResourceArns(id)
                .withTags(tag);
        elb.addTags(tagsRequest);
        logger.info("Tagged: {} with key: {} value: {}", id, key, value);
    }

    public List<String> getIdsForLoadBalancersWithoutTag(String tag) {
        logger.trace("Describing Load Balancers");
        List<String> untaggedLoadBalancerIds = new ArrayList<>();
        DescribeLoadBalancersResult result = elb.describeLoadBalancers(new DescribeLoadBalancersRequest());
        for (LoadBalancer loadBalancer : result.getLoadBalancers()) {
            DescribeTagsRequest tagsRequest = new DescribeTagsRequest()
                    .withResourceArns(loadBalancer.getLoadBalancerArn());
            DescribeTagsResult tagsResult = elb.describeTags(tagsRequest);
            for (TagDescription tagDescription : tagsResult.getTagDescriptions()) {
                if (!hasTag(tagDescription, tag)) {
                    untaggedLoadBalancerIds.add(loadBalancer.getLoadBalancerArn());
                }
            }
        }
        logger.info("Found {} LoadBalancers without: {} on AWS", untaggedLoadBalancerIds.size(), tag);
        untaggedLoadBalancerIds.forEach(logger::info);
        return untaggedLoadBalancerIds;
    }

    private boolean hasTag(TagDescription tagDescription, String tag) {
        logger.debug(tag + " found: " + tagDescription.getTags().stream().anyMatch(t -> t.getKey().equals(tag)));
        return tagDescription.getTags().stream().anyMatch(t -> t.getKey().equals(tag));
    }
}
