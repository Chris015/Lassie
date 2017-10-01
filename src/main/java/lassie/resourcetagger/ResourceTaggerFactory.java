package lassie.resourcetagger;

import lassie.Application;
import lassie.awshandlers.*;
import lassie.mocks.*;

public class ResourceTaggerFactory {

    private Ec2Handler ec2Handler;
    private EMRHandler emrHandler;
    private RedshiftHandler redshiftHandler;
    private ELBHandler elbHandler;
    private S3Handler s3Handler;
    private RDSHandler rdsHandler;

    public ResourceTaggerFactory() {
        if (!Application.mockMode) {
            this.ec2Handler = new EC2HandlerImpl();
            this.emrHandler = new EMRHandlerImpl();
            this.redshiftHandler = new RedshiftHandlerImpl();
            this.elbHandler = new ELBHandlerImpl();
            this.s3Handler = new S3HandlerImpl();
            this.rdsHandler = new RDSHandlerImpl();
        } else {
            this.ec2Handler = new EC2HandlerMock();
            this.emrHandler = new EMRHandlerMock();
            this.redshiftHandler = new RedshiftHandlerMock();
            this.elbHandler = new ELBHandlerMock();
            this.s3Handler = new S3HandlerMock();
            this.rdsHandler = new RDSHandlerMock();
        }
    }

    public ResourceTagger getResourceTagger(String resourceType) throws UnsupportedResourceTypeException {
        switch (resourceType.toUpperCase()) {
            case "SECURITYGROUP":
                return new SecurityGroupTagger(ec2Handler);
            case "S3BUCKET":
                return new S3BucketTagger(s3Handler);
            case "EBSVOLUME":
                return new EBSVolumeTagger(ec2Handler);
            case "RDSDBINSTANCE":
                return new RDSDBInstanceTagger(rdsHandler);
            case "REDSHIFTCLUSTER":
                return new RedshiftClusterTagger(redshiftHandler);
            case "EC2INSTANCE":
                return new EC2InstanceTagger(ec2Handler);
            case "EMRCLUSTER":
                return new EMRClusterTagger(emrHandler);
            case "LOADBALANCER":
                return new LoadBalancerTagger(elbHandler);
        }
        throw new UnsupportedResourceTypeException(resourceType + " is not a supported resource type");
    }

    public void setEc2Handler(Ec2Handler ec2Handler) {
        this.ec2Handler = ec2Handler;
    }
}