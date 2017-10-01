package lassie;

import lassie.awshandlers.Ec2Handler;
import lassie.config.Account;
import lassie.mocks.ConfigReaderMock;
import lassie.mocks.EC2HandlerMock;
import lassie.mocks.LogFetcherMock;
import lassie.model.Log;
import lassie.resourcetagger.EC2InstanceTagger;
import lassie.resourcetagger.ResourceTaggerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class LassieIT {
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
    public void oneUntaggedInstance_invokedTagResourcesOnce() throws Exception {
        //given
        prepareTaggingEc2InstancesTest();

        List<String> ids = new ArrayList<>();
        ids.add("i-06e9aaf9760467624");
        EC2HandlerMock.idsForInstancesWithoutTags = ids;

        //when
        application.run(new String[0]);

        verify(ec2Handler, times(1))
                .tagResource("i-06e9aaf9760467624","Owner", "john.doe");
        verify(ec2Handler, times(0))
                .tagResource("i-07a5bbg4326310341", "Owner", "jane.doe");
    }

    @Test
    public void twoUntaggedInstances_invokesTagResourcesTwice() throws Exception {
        //given
        prepareTaggingEc2InstancesTest();

        List<String> ids = new ArrayList<>();
        ids.add("i-06e9aaf9760467624");
        ids.add("i-07a5bbg4326310341");
        EC2HandlerMock.idsForInstancesWithoutTags = ids;

        //when
        application.run(new String[0]);

        // then
        verify(ec2Handler, times(1))
                .tagResource("i-06e9aaf9760467624", "Owner", "john.doe");

        verify(ec2Handler, times(1))
                .tagResource("i-07a5bbg4326310341", "Owner", "jane.doe");
    }

    private void prepareTaggingEc2InstancesTest() {
        List<Account> accounts = new ArrayList<>();
        Account account = createAccount(Arrays.asList("ec2instance"));
        accounts.add(account);

        ConfigReaderMock.accounts = accounts;

        List<String> filePaths = new ArrayList<>();
        filePaths.add(ClassLoader.getSystemResource("ec2Instances.json").getPath());

        List<Log> logs = new ArrayList<>();
        Log log = new Log(account, filePaths);
        logs.add(log);
        LogFetcherMock.logs = logs;
    }

    private Account createAccount(List<String> resourceTypes) {
        Account account = new Account();
        account.setSecretAccessKey("");
        account.setAccessKeyId("");
        account.setOwnerTag("Owner");
        account.setRegions(Arrays.asList(""));
        account.setResourceTypes(resourceTypes);
        return account;
    }
}
