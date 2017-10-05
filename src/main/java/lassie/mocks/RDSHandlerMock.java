package lassie.mocks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RDSHandlerMock implements lassie.awshandlers.RDSHandler {
    private static final Logger logger = LogManager.getLogger(RDSHandlerMock.class);
    public static List<String> dbInstancesWithoutTag = new ArrayList<>();
    public static List <String> dbInstancesWithTag = new ArrayList<>();

    @Override
    public void instantiateRDSClient(String accessKeyId, String secretAccessKey, String region) {
    }

    @Override
    public void tagResource(String id, String key, String value) {
        logger.info("Tagged: {} with key: {} value: {}", id, key, value);
    }

    @Override
    public List<String> getIdsForDBInstancesWithoutTag(String tag) {
        return dbInstancesWithoutTag;
    }
}
