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
    public static boolean mockMode = false;
    public static boolean DRY_RUN;
    private final DateInterpreter dateInterpreter;
    private ConfigReader configReader;
    private LogFetcher logFetcher;
    private ResourceTaggerFactory resourceTaggerFactory;

    public Application() {
        log.info("Application started");
        String mockModeProperty = System.getProperties().getProperty("mockmode");
        if (mockModeProperty != null)
            mockMode = System.getProperties().getProperty("mockmode").equals("true");
        this.configReader = mockMode ? new ConfigReaderMock() : new ConfigReaderImpl();
        DRY_RUN = configReader.getDryRun();
        this.logFetcher = mockMode ? new LogFetcherMock() : new S3LogFetcher();
        this.resourceTaggerFactory = !mockMode ? new ResourceTaggerFactory() : null;
        this.dateInterpreter = new DateInterpreter();
    }

    public void run(String[] args) {
        List<Account> accounts = configReader.getAccounts();
        logFetcher.createTmpFolders();
        String fromDate = dateInterpreter.interpret(args);
        List<Log> logs = logFetcher.getLogs(fromDate, accounts);
        tagResources(logs);
        logFetcher.clearLogs();
        log.info("Application completed");
    }

    private void tagResources(List<Log> logs) {
        for (Log log : logs) {
            Account account = log.getAccount();
            List<String> resourceTypes = account.getResourceTypes();
            List<ResourceTagger> resourceTaggers = createResourceTaggers(resourceTypes);

            for (ResourceTagger resourceTagger : resourceTaggers) {
                resourceTagger.tagResources(log);
            }
        }
    }

    private List<ResourceTagger> createResourceTaggers(List<String> resourceTypes) {
        if (resourceTypes == null)
            throw new IllegalArgumentException("No resource types found in config file");

        log.info("Creating resource taggers");
        List<ResourceTagger> resourceTaggers = new ArrayList<>();
        for (String resourceType : resourceTypes) {
            try {
                resourceTaggers.add(resourceTaggerFactory.getResourceTagger(resourceType));
            } catch (UnsupportedResourceTypeException e) {
                log.warn("Unsupported resource request.", e);
                e.printStackTrace();
            }
        }
        log.info("Resource taggers created");
        return resourceTaggers;
    }

    public void setResourceTaggerFactory(ResourceTaggerFactory resourceTaggerFactory) {
        this.resourceTaggerFactory = resourceTaggerFactory;
    }
}
