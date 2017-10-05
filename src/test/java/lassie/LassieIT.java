package lassie;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.elasticmapreduce.model.Cluster;
import lassie.awshandlers.*;
import lassie.config.Account;
import lassie.mocks.*;
import lassie.model.Log;
import lassie.resourcetagger.ResourceTaggerFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class LassieIT {
    private static final String OWNER_TAG = "Owner";
    private String[] args = new String[0];
    private Application application;
    private Ec2Handler ec2Handler;
    private EMRHandler emrHandler;
    private RedshiftHandler redshiftHandler;
    private ELBHandler elbHandler;
    private S3Handler s3Handler;
    private RDSHandler rdsHandler;

    @Before
    public void setUp() throws Exception {
        this.ec2Handler = spy(new EC2HandlerMock());
        this.emrHandler = spy(new EMRHandlerMock());
        this.redshiftHandler = spy(new RedshiftHandlerMock());
        this.elbHandler = spy(new ELBHandlerMock());
        this.s3Handler = spy(new S3HandlerMock());
        this.rdsHandler = spy(new RDSHandlerMock());

        ResourceTaggerFactory resourceTaggerFactory = new ResourceTaggerFactory();
        resourceTaggerFactory.setEc2Handler(ec2Handler);
        resourceTaggerFactory.setEmrHandler(emrHandler);
        resourceTaggerFactory.setRedshiftHandler(redshiftHandler);
        resourceTaggerFactory.setElbHandler(elbHandler);
        resourceTaggerFactory.setS3Handler(s3Handler);
        resourceTaggerFactory.setRdsHandler(rdsHandler);

        this.application = new Application();
        this.application.setResourceTaggerFactory(resourceTaggerFactory);
    }

    @Test
    public void untaggedInstances_areTagged() throws Exception {
        //given
        prepareTest("ec2instance", "ec2instances.json");

        HashMap<String, Instance> instances = EC2HandlerMock.instances;
        Instance instance = instances.get("i-07a5bbg4326310341");
        assertEquals(0, instance.getTags().size());

        instance = instances.get("i-06e9aaf9760467624");
        assertEquals(1, instance.getTags().size());

        //when
        application.run(args);

        //then
        verify(ec2Handler, times(1))
                .tagResource("i-07a5bbg4326310341", OWNER_TAG, "jane.doe");
        verify(ec2Handler, times(0))
                .tagResource("i-06e9aaf9760467624", OWNER_TAG, "john.doe");

        assertEquals(1, instance.getTags().size());

        Tag tag = instances.get("i-07a5bbg4326310341").getTags().get(0);
        assertEquals(OWNER_TAG, tag.getKey());
        assertEquals("jane.doe", tag.getValue());
    }

    @Test
    public void untaggedSecurityGroups_areTagged() throws Exception {
        //given
        prepareTest("securitygroup", "securitygroups.json");

        HashMap<String, SecurityGroup> securityGroups = EC2HandlerMock.securityGroups;
        SecurityGroup securityGroup = securityGroups.get("s-9831ba2f192s2");
        assertEquals(1, securityGroup.getTags().size());

        securityGroup = securityGroups.get("s-1214cb1l323b1");
        assertEquals(0, securityGroup.getTags().size());

        //when
        application.run(args);

        //then
        verify(ec2Handler, times(1))
                .tagResource("s-1214cb1l323b1", OWNER_TAG, "johnny.doe");
        verify(ec2Handler, times(0))
                .tagResource("s-9831ba2f192s2", OWNER_TAG, "jane.doe");

        Tag tag = securityGroups.get("s-1214cb1l323b1").getTags().get(0);
        assertEquals(OWNER_TAG, tag.getKey());
        assertEquals("johnny.doe", tag.getValue());
    }

    @Test
    public void untaggedVolumes_areTagged() throws Exception {
        //given
        prepareTest("ebsvolume", "ebsvolumes.json");

        HashMap<String, Volume> volumes = EC2HandlerMock.volumes;
        Volume volume = volumes.get("v-203412c121a31");
        assertEquals(0, volume.getTags().size());

        volume = volumes.get("v-313821c242b32");
        assertEquals(0, volume.getTags().size());
        assertEquals(1, volume.getAttachments().size());

        volume = volumes.get("v-109812b123a21");
        assertEquals(1, volume.getTags().size());

        //when
        application.run(args);

        //then
        verify(ec2Handler, times(1))
                .tagResource("v-203412c121a31", OWNER_TAG, "johnny.doe");
        verify(ec2Handler, times(1))
                .tagResource("v-313821c242b32", OWNER_TAG, "john.doe");
        verify(ec2Handler, times(0))
                .tagResource("v-109812b123a21", OWNER_TAG, "jane.doe");

        Tag tag = volumes.get("v-203412c121a31").getTags().get(0);
        assertEquals(OWNER_TAG, tag.getKey());
        assertEquals("johnny.doe", tag.getValue());

        tag = volumes.get("v-313821c242b32").getTags().get(0);
        assertEquals(OWNER_TAG, tag.getKey());
        assertEquals("john.doe", tag.getValue());
    }

    @Test
    public void untaggedEMRClusters_areTagged() throws Exception {
        //given
        prepareTest("emrcluster", "emrclusters.json");
        HashMap<String, Cluster> clusters = EMRHandlerMock.clusters;
        Cluster cluster = clusters.get("j-123c12b123a12");
        assertEquals(1, cluster.getTags().size());

        cluster = clusters.get("j-321a21c321b21");
        assertEquals(0, cluster.getTags().size());

        //when
        application.run(args);

        //then
        verify(emrHandler, times(0))
                .tagResource("j-123c12b123a12", OWNER_TAG, "jane.doe");
        verify(emrHandler, times(1))
                .tagResource("j-321a21c321b21", OWNER_TAG, "johnny.doe");

        com.amazonaws.services.elasticmapreduce.model.Tag tag = clusters.get("j-321a21c321b21").getTags().get(0);
        assertEquals(OWNER_TAG, tag.getKey());
        assertEquals("johnny.doe", tag.getValue());
    }

    @Test
    public void untaggedRedshiftClusters_areTagged() throws Exception {
        //given
        prepareTest("redshiftcluster", "redshiftclusters.json");
        HashMap<String, com.amazonaws.services.redshift.model.Cluster> clusters = RedshiftHandlerMock.clusters;

        com.amazonaws.services.redshift.model.Cluster cluster = clusters.get("arn:aws:redshift:ap-south-1:12345:cluster:r-109812b123a21");
        assertEquals(1, cluster.getTags().size());

        cluster = clusters.get("arn:aws:redshift:ap-south-1:12345:cluster:r-203412c121a31");
        assertEquals(0, cluster.getTags().size());

        //when
        application.run(args);

        //then
        verify(redshiftHandler, times(0))
                .tagResource("arn:aws:redshift:ap-south-1:12345:cluster:r-109812b123a21", OWNER_TAG, "jane.doe");
        verify(redshiftHandler, times(1))
                .tagResource("arn:aws:redshift:ap-south-1:12345:cluster:r-203412c121a31", OWNER_TAG, "johnny.doe");

        com.amazonaws.services.redshift.model.Tag tag = clusters.get("arn:aws:redshift:ap-south-1:12345:cluster:r-203412c121a31").getTags().get(0);
        assertEquals(OWNER_TAG, tag.getKey());
        assertEquals("johnny.doe", tag.getValue());
    }

    @Test
    public void untaggedLoadBalancers_areTagged() throws Exception {
        //given
        prepareTest("loadbalancer", "loadbalancers.json");
        List<String> loadBalancersWithTag = new ArrayList<>();
        loadBalancersWithTag.add("arn:aws:elasticloadbalancing:ap-south-1:12345:loadbalancer/app/test/d21fc16f3efsa23a");
        ELBHandlerMock.loadBalancersWithTag = loadBalancersWithTag;

        List<String> loadBalancersWithoutTag = new ArrayList<>();
        loadBalancersWithoutTag.add("arn:aws:elasticloadbalancing:ap-south-1:12345:loadbalancer/app/test/e7d9fcc49ebff18b");
        ELBHandlerMock.loadBalancersWithoutTag = loadBalancersWithoutTag;

        //when
        application.run(args);

        //then
        verify(elbHandler, times(1))
                .tagResource(loadBalancersWithoutTag.get(0), OWNER_TAG, "johnny.doe");
        verify(elbHandler, times(0))
                .tagResource(loadBalancersWithTag.get(0), OWNER_TAG, "jane.doe");
    }

    @Test
    public void untaggedDBInstance_areTagged() throws Exception {
        //given
        prepareTest("rdsdbinstance", "rdsdbinstances.json");
        List<String> untaggedDBInstances = new ArrayList<>();
        untaggedDBInstances.add("arn:aws:rds:eu-west-1:123456789012:db:mysql-db1");
        RDSHandlerMock.dbInstancesWithoutTag = untaggedDBInstances;

        List<String> taggedDBInstances = new ArrayList<>();
        taggedDBInstances.add("arn:aws:rds:eu-west-1:123456789012:db:mysql-db2");
        RDSHandlerMock.dbInstancesWithTag = taggedDBInstances;

        //when
        application.run(args);

        //then
        verify(rdsHandler, times(1))
                .tagResource(untaggedDBInstances.get(0), OWNER_TAG, "jane.doe");
        verify(rdsHandler, times(0))
                .tagResource(taggedDBInstances.get(0), OWNER_TAG, "johnny.doe");
    }

    @Test
    public void untagged3Buckets_areTagged() throws Exception {
        //given
        prepareTest("s3bucket", "s3buckets.json");

        List<String> bucketsWithTag = new ArrayList<>();
        bucketsWithTag.add("BucketOne");
        S3HandlerMock.bucketsWithTag = bucketsWithTag;

        List<String> bucketsWithoutTag = new ArrayList<>();
        bucketsWithoutTag.add("BucketTwo");
        S3HandlerMock.bucketsWithoutTag = bucketsWithoutTag;

        //when
        application.run(args);

        //then
        verify(s3Handler, times(1))
                .tagBucket("BucketTwo", OWNER_TAG, "johnny.doe");
        verify(s3Handler, times(0))
                .tagBucket("BucketOne", OWNER_TAG, "jane.doe");
    }

    private void prepareTest(String resourceType, String fileName) {
        List<Account> accounts = new ArrayList<>();
        Account account = createAccount(singletonList(resourceType));
        accounts.add(account);

        ConfigReaderMock.accounts = accounts;

        List<String> filePaths = new ArrayList<>();
        filePaths.add(ClassLoader.getSystemResource(fileName).getPath());

        Log log = new Log(account.getRegions().get(0), filePaths);
        account.addLog(log);
    }

    private Account createAccount(List<String> resourceTypes) {
        Account account = new Account();
        account.setSecretAccessKey("");
        account.setAccessKeyId("");
        account.setAccountId("12345");
        account.setOwnerTag(OWNER_TAG);
        account.setRegions(singletonList("ap-south-1"));
        account.setResourceTypes(resourceTypes);
        return account;
    }
}
