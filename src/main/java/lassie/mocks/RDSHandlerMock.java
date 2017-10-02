package lassie.mocks;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RDSHandlerMock implements lassie.awshandlers.RDSHandler {
    private final static Logger log = Logger.getLogger(RDSHandlerMock.class);
    public static List<String> dbInstancesWithoutTag = new ArrayList<>();
    public static List <String> dbInstancesWithTag = new ArrayList<>();

    @Override
    public void instantiateRDSClient(String accessKeyId, String secretAccessKey, String region) {
    }

    @Override
    public void tagResource(String id, String key, String value) {
        log.info("Tagged: " + id + " with key: " + key + " value: " + value);
    }

    @Override
    public List<String> getIdsForDBInstancesWithoutTag(String tag) {
        return dbInstancesWithoutTag;
    }
}
