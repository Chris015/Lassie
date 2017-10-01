package lassie;

import lassie.config.Account;
import lassie.config.ConfigReader;
import lassie.config.ConfigReaderImpl;
import lassie.mocks.ConfigReaderMock;
import lassie.mocks.LogFetcherMock;
import lassie.model.Log;
import lassie.resourcetagger.ResourceTagger;
import lassie.resourcetagger.ResourceTaggerFactory;
import lassie.resourcetagger.UnsupportedResourceTypeException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Application {
    private final static Logger log = Logger.getLogger(Application.class);
    public static boolean mockMode = System.getProperties().getProperty("mockmode").equals("true");
    public static boolean DRY_RUN;
    private final DateInterpreter dateInterpreter;
    private ConfigReader configReader;
    private LogFetcher logFetcher;
    private ResourceTaggerFactory resourceTaggerFactory;
    private String fromDate;

    public Application() {
        log.info("Application started");
        this.configReader = mockMode ? new ConfigReaderMock() : new ConfigReaderImpl();
        DRY_RUN = configReader.getDryRun();
        this.logFetcher = mockMode ? new LogFetcherMock() : new S3LogFetcher();
        this.resourceTaggerFactory = !mockMode ? new ResourceTaggerFactory() : null;
        this.dateInterpreter = new DateInterpreter();
    }

    public void run(String[] args) {
        this.fromDate = dateInterpreter.interpret(args);
        List<Account> accounts = configReader.getAccounts();
        List<ResourceTagger> resourceTaggers = createResourceTaggers(accounts);
        logFetcher.createTmpFolders();
        List<Log> logs = logFetcher.getLogs(fromDate, accounts);
        resourceTaggers.forEach(resourceTagger -> resourceTagger.tagResources(logs));
        logFetcher.clearLogs();
        log.info("Application completed");
    }

    private List<ResourceTagger> createResourceTaggers(List<Account> accounts) {
        log.info("Creating resource taggers");
        List<ResourceTagger> resourceTaggers = new ArrayList<>();
        List<String> resourceTypes = new ArrayList<>();

        for (Account account : accounts) {
            resourceTypes.addAll(account.getResourceTypes());
        }
        try {
            for (String resourceType : resourceTypes) {
                resourceTaggers.add(resourceTaggerFactory.getResourceTagger(resourceType));
            }
        } catch (UnsupportedResourceTypeException e) {
            log.warn("Unsupported resource request.", e);
            e.printStackTrace();
        }
        log.info("Resource taggers created");
        return resourceTaggers;
    }

    public void setResourceTaggerFactory(ResourceTaggerFactory resourceTaggerFactory) {
        this.resourceTaggerFactory = resourceTaggerFactory;
    }
}
