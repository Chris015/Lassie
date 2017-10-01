package lassie.mocks;

import java.util.List;

public class ELBHandlerMock implements lassie.awshandlers.ELBHandler {
    @Override
    public void instantiateELBClient(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagResource(String id, String key, String value) {

    }

    @Override
    public List<String> getIdsForLoadBalancersWithoutTag(String tag) {
        return null;
    }
}
