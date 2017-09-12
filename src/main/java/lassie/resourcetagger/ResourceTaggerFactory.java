package lassie.resourcetagger;

public class ResourceTaggerFactory {

    public ResourceTagger getResourceTagger(String resourceType) throws UnsupportedResourceTypeException {
        switch (resourceType.toUpperCase()) {
            case "RUNINSTANCES": return new RunInstancesTagger();
            case "CREATESECURITYGROUP": return new SecurityGroupTagger();
            case "CREATEBUCKET": return new S3BucketTagger();
            case "CREATEVOLUME": return new EBSVolumeTagger();
            case "CREATEDBINSTANCE": return  new RDSDBInstanceTagger();
            case "RUNJOBFLOW": return new RunJobFlowTagger();
            case "CREATECLUSTER": return new RedshiftClusterTagger();
            case "CREATELOADBALANCER": return new LoadBalancerTagger();
        }
        throw new UnsupportedResourceTypeException(resourceType + " is not a supported");
    }

}
