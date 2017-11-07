package lassie;

import lassie.config.Account;
import lassie.config.Config;
import lassie.config.ConfigReader;
import lassie.config.YamlReader;
import lassie.mocks.ConfigReaderMock;
import lassie.mocks.LogFetcherMock;
import lassie.resourcetagger.ResourceTagger;
import lassie.resourcetagger.ResourceTaggerFactory;
import lassie.resourcetagger.UnsupportedResourceTypeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Application {
    private final static Logger logger = LogManager.getLogger(Application.class);
    public static boolean mockMode = false;
    public static boolean DRY_RUN;
    private final DateInterpreter dateInterpreter;
    private LogFetcher logFetcher;
    private ResourceTaggerFactory resourceTaggerFactory;
    private Config config;

    public Application() {
        logger.info("Application started");
        String mockModeProperty = System.getProperties().getProperty("mockmode");
        if (mockModeProperty != null)
            mockMode = System.getProperties().getProperty("mockmode").equals("true");
        ConfigReader configReader = mockMode ? new ConfigReaderMock() : new YamlReader();
        this.config = configReader.readConfiguration();
        DRY_RUN = config.isDryRun();
        this.logFetcher = mockMode ? new LogFetcherMock() : new S3LogFetcher();
        this.resourceTaggerFactory = !mockMode ? new ResourceTaggerFactory() : null;
        this.dateInterpreter = new DateInterpreter();
    }

    public void run(String[] args) {
        List<Account> accounts = config.getAccounts();
        setDefaultConfigIfNotOverridden(accounts);
        logFetcher.createTmpFolders();
        String fromDate = dateInterpreter.interpret(args);
        logFetcher.addLogsToAccount(fromDate, accounts);
        tagResources(accounts);
        logFetcher.clearLogs();
        logger.info("Application completed");
    }

    private void setDefaultConfigIfNotOverridden(List<Account> accounts) {
        for (Account account : accounts) {
            if (account.getOwnerTag() == null) {
                account.setOwnerTag(config.getOwnerTag());
            }
            if (account.getResourceTypes() == null) {
                account.setResourceTypes(config.getResourceTypes());
            }
            if (account.getRegions() == null) {
                account.setRegions(config.getRegions());
            }
        }
    }

    private void tagResources(List<Account> accounts) {
        for (Account account : accounts) {
            List<String> resourceTypes = account.getResourceTypes();
            resourceTypes = (resourceTypes == null) ? config.getResourceTypes() : resourceTypes;

            List<ResourceTagger> resourceTaggers = createResourceTaggers(resourceTypes);

            for (ResourceTagger resourceTagger : resourceTaggers) {
                resourceTagger.tagResources(account);
            }
        }

    }

    private List<ResourceTagger> createResourceTaggers(List<String> resourceTypes) {
        if (resourceTypes == null)
            throw new IllegalArgumentException("No resource types found in config file");

        logger.info("Creating resource taggers");
        List<ResourceTagger> resourceTaggers = new ArrayList<>();
        for (String resourceType : resourceTypes) {
            try {
                resourceTaggers.add(resourceTaggerFactory.getResourceTagger(resourceType));
            } catch (UnsupportedResourceTypeException e) {
                logger.warn("Unsupported resource type: ", e);
                e.printStackTrace();
            }
        }
        logger.info("Resource taggers created");
        return resourceTaggers;
    }

    public void setResourceTaggerFactory(ResourceTaggerFactory resourceTaggerFactory) {
        this.resourceTaggerFactory = resourceTaggerFactory;
    }
}
