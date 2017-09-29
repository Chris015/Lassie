package lassie.awshandlers;

import java.util.List;

public interface ELBHandler {

    void instantiateELBClient(String accessKeyId, String secretAccessKey, String region);

    void tagResource(String id, String key, String value);

    List<String> getIdsForLoadBalancersWithoutTag(String tag);
}
