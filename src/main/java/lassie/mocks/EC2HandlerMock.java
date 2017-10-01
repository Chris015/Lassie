package lassie.mocks;

import java.util.ArrayList;
import java.util.List;

public class EC2HandlerMock implements lassie.awshandlers.Ec2Handler {
    public static List<String> idsForInstancesWithoutTags = new ArrayList<>();

    @Override
    public void instantiateEC2Client(String accessKeyId, String secretAccessKey, String region) {
    }

    @Override
    public void tagResource(String id, String key, String value) {

    }

    @Override
    public String getTagValueForInstanceWithId(String tagKey, String instanceId) {
        return null;
    }

    @Override
    public List<String> getIdsForInstancesWithoutTag(String tag) {
        return idsForInstancesWithoutTags;
    }

    @Override
    public List<String> getIdsForSecurityGroupsWithoutTag(String tag) {
        return null;
    }

    @Override
    public List<String> getIdsForVolumesWithoutTag(String tag) {
        return null;
    }

    @Override
    public boolean volumeIsAttachedToInstance(String volumeId) {
        return false;
    }

    @Override
    public String getIdForInstanceVolumeIsAttachedTo(String volumeId) {
        return null;
    }
}
