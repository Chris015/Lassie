package lassie.awshandlers;

import java.util.List;

public interface Ec2Handler {
    void instantiateEC2Client(String accessKeyId, String secretAccessKey, String region);

    void tagResource(String id, String key, String value);

    String getTagValueForInstanceWithId(String tagKey, String instanceId);

    List<String> getIdsForInstancesWithoutTag(String tag);

    List<String> getIdsForSecurityGroupsWithoutTag(String tag);

    List<String> getIdsForVolumesWithoutTag(String tag);

    boolean volumeIsAttachedToInstance(String volumeId);

    String getIdForInstanceVolumeIsAttachedTo(String volumeId);
}
