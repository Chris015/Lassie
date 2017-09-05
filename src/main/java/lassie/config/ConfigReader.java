package lassie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
import java.util.List;

public class ConfigReader {

    ObjectMapper objectMapper;

    public ConfigReader() {
        objectMapper = new ObjectMapper(new YAMLFactory());
    }

    public List<AccountConfig> readConfig() {
        try {
            return objectMapper.readValue(
                    new File(ClassLoader.getSystemResource("configuration.yaml").getPath()),
                    AccountsConfig.class).getAccounts();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
