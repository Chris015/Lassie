package lassie.resourcetagger;

import lassie.awsHandlers.EC2Handler;
import lassie.awsHandlers.EMRHandler;
import lassie.awsHandlers.S3Handler;

public class ResourceTaggerFactory {

    private S3Handler s3Handler = new S3Handler();
    private EC2Handler ec2Handler = new EC2Handler();
    private EMRHandler emrHandler = new EMRHandler();

    public ResourceTagger getResourceTagger(String resourceType) throws UnsupportedResourceTypeException {
        switch (resourceType.toUpperCase()) {
            case "SECURITYGROUP":
                return new SecurityGroupTagger();
            case "S3BUCKET":
                return new S3BucketTagger(s3Handler);
            case "EBSVOLUME":
                return new EBSVolumeTagger(ec2Handler);
            case "RDSDBINSTANCE":
                return new RDSDBInstanceTagger();
            case "REDSHIFTCLUSTER":
                return new RedshiftClusterTagger();
            case "EC2INSTANCE":
                return new EC2InstanceTagger(ec2Handler);
            case "EMRCLUSTER":
                return new EMRClusterTagger(emrHandler);
            case "LOADBALANCER":
                return new LoadBalancerTagger();
        }
        throw new UnsupportedResourceTypeException(resourceType + " is not a supported resource type");
    }
}
