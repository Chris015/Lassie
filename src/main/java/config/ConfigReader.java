package config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;

public class ConfigReader {

    ObjectMapper objectMapper;

    public ConfigReader() {
        objectMapper = new ObjectMapper(new YAMLFactory());
    }

    public AccountsConfig readConfig() {
        try {
            return objectMapper.readValue(new File("/Users/praktikant/Repos/aws-tagging-service/src/main/resources/configuration.yaml"), AccountsConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
