package lassie.mocks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ELBHandlerMock implements lassie.awshandlers.ELBHandler {
    private static final Logger logger = LogManager.getLogger(ELBHandlerMock.class);
    public static List<String> loadBalancersWithTag = new ArrayList<>();
    public static List<String> loadBalancersWithoutTag = new ArrayList<>();


    @Override
    public void instantiateELBClient(String accessKeyId, String secretAccessKey, String region) {

    }

    @Override
    public void tagResource(String id, String key, String value) {
        logger.info("Tagged: {} with key: {} value: {}", id, key, value);
    }

    @Override
    public List<String> getIdsForLoadBalancersWithoutTag(String tag) {
        return loadBalancersWithoutTag;
    }
}
