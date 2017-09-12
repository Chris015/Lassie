package lassie.resourcetagger;

public class ResourceTaggerFactory {

    public ResourceTagger getResourceTagger(String resourceType) throws UnsupportedResourceTypeException {
        switch (resourceType.toUpperCase()) {
            case "SECURITYGROUP":
                return new SecurityGroupTagger();
            case "S3BUCKET":
                return new S3BucketTagger();
            case "EBSVOLUME":
                return new EBSVolumeTagger();
            case "RDSDBINSTANCE":
                return new RDSDBInstanceTagger();
            case "REDSHIFTCLUSTER":
                return new RedshiftClusterTagger();
            case "EC2INSTANCE":
                return new EC2Instance();
            case "EMRCLUSTER":
                return new EMRClusterTagger();
            case "LOADBALANCER":
                return new LoadBalancerTagger();
        }
        throw new UnsupportedResourceTypeException(resourceType + " is not a supported");
    }
}
