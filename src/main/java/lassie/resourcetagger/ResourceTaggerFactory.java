package lassie.resourcetagger;

public class ResourceTaggerFactory {

    public ResourceTagger getResourceTagger(String resourceType) throws UnsupportedResourceTypeException {
        switch (resourceType.toUpperCase()) {
            case "RUNINSTANCES": return new EC2Instance();
            case "CREATESECURITYGROUP": return new CreateSecurityGroupTagger();
            case "CREATEBUCKET": return new S3BucketTagger();
            case "CREATEVOLUME": return new EBSVolumeTagger();
            case "CREATEDBINSTANCE": return  new CreateDBInstanceTagger();
            case "RUNJOBFLOW": return new EMRClusterTagger();
            case "CREATECLUSTER": return new ClusterTagger();
            case "CREATELOADBALANCER": return new LoadBalancerTagger();
        }
        throw new UnsupportedResourceTypeException(resourceType + " is not a supported");
    }

}
