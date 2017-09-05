import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import config.AccountConfig;
import config.ConfigReader;

import java.util.List;

public class Application {

    private ConfigReader configReader;
    private List<AccountConfig> accounts;
    private AmazonS3 s3;
    private AmazonEC2 ec2;
    private TagInterpreter tagInterpreter;
    private EventInterpreter eventInterpreter;
    private DateFormatter dateFormatter;
    private EventHandler eventHandler;

    public Application() {
        this.configReader = new ConfigReader();
        this.accounts = configReader.readConfig();
    }

    public void run(){
        for (AccountConfig account : accounts) {
            for (String region : account.getRegions()) {
                createAmazonClients(account, region);
                instantiateClasses(account);
                eventHandler.fetchUntaggedEvents();
            }
        }
    }

    private void instantiateClasses(AccountConfig account) {
        this.tagInterpreter = new TagInterpreter(ec2);
        this.eventInterpreter = new EventInterpreter(ec2, tagInterpreter);
        this.dateFormatter = new DateFormatter();
        this.eventHandler = new EventHandler(eventInterpreter, account.getEvents());

    }

    private void createAmazonClients(AccountConfig account, String region) {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(account.getAccessKeyId(),
                account.getSecretAccessKey());

        this.s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.fromName(region))
                .build();

        this.ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion(region)
                .build();
    }


}
