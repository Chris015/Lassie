package lassie.mocks;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ELBHandlerMock implements lassie.awshandlers.ELBHandler {
    private final static Logger log = Logger.getLogger(ELBHandlerMock.class);
    public static List<String> loadBalancersWithTag = new ArrayList<>();
    public static List<String> loadBalancersWithoutTag = new ArrayList<>();


    @Override
    public void instantiateELBClient(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagResource(String id, String key, String value) {
        log.info("Tagged: " + id + " with key: " + key + " value: " + value);
    }

    @Override
    public List<String> getIdsForLoadBalancersWithoutTag(String tag) {
        return loadBalancersWithoutTag;
    }
}
