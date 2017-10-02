package lassie;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import lassie.awshandlers.Ec2Handler;
import lassie.config.Account;
import lassie.mocks.ConfigReaderMock;
import lassie.mocks.EC2HandlerMock;
import lassie.mocks.LogFetcherMock;
import lassie.model.Log;
import lassie.resourcetagger.ResourceTaggerFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LassieIT {
    private static final String OWNER_TAG = "Owner";
    private String[] args = new String[0];
    private Application application;
    private Ec2Handler ec2Handler;

    @Before
    public void setUp() throws Exception {
        this.ec2Handler = spy(new EC2HandlerMock());
        ResourceTaggerFactory resourceTaggerFactory = new ResourceTaggerFactory();
        resourceTaggerFactory.setEc2Handler(ec2Handler);

        this.application = new Application();
        this.application.setResourceTaggerFactory(resourceTaggerFactory);
    }

    @Test
    public void untaggedInstances_areTagged() throws Exception {
        //given
        prepareEc2Test("ec2instance", "ec2Instances.json");

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
        prepareEc2Test("securitygroup", "securitygroups.json");

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
        prepareEc2Test("ebsvolume", "ebsvolumes.json");

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

    private void prepareEc2Test(String resourceType, String fileName) {
        List<Account> accounts = new ArrayList<>();
        Account account = createAccount(singletonList(resourceType));
        accounts.add(account);

        ConfigReaderMock.accounts = accounts;

        List<String> filePaths = new ArrayList<>();
        filePaths.add(ClassLoader.getSystemResource(fileName).getPath());

        List<Log> logs = new ArrayList<>();
        Log log = new Log(account, filePaths);
        logs.add(log);
        LogFetcherMock.logs = logs;
    }

    private Account createAccount(List<String> resourceTypes) {
        Account account = new Account();
        account.setSecretAccessKey("");
        account.setAccessKeyId("");
        account.setOwnerTag(OWNER_TAG);
        account.setRegions(singletonList(""));
        account.setResourceTypes(resourceTypes);
        return account;
    }
}
