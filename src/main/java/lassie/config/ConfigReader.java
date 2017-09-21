package lassie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigReader {
    private static Logger log = Logger.getLogger(ConfigReader.class);
    private ObjectMapper objectMapper;

    public ConfigReader() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
    }

    public List<Account> getAccounts() {
        try {
            return objectMapper.readValue(
                    new File("configuration.yaml"),
                    AccountsConfig.class).getAccounts();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}