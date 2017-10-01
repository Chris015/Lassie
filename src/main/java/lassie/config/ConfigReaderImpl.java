package lassie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigReaderImpl implements ConfigReader {
    private ObjectMapper objectMapper;
    private File configurationFile = new File("configuration.yaml");

    public ConfigReaderImpl() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
    }

    public List<Account> getAccounts() {
        return getAccountConfig().getAccounts();
    }

    public boolean getDryRun() {
        return getAccountConfig().isDryRun();
    }

    private AccountsConfig getAccountConfig() {
        try {
            return objectMapper.readValue(
                    configurationFile,
                    AccountsConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new NullPointerException("Unable to get Account Config");
    }
}