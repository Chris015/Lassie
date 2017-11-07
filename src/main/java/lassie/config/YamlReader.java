package lassie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class YamlReader implements ConfigReader {
    private ObjectMapper objectMapper;
    private File configurationFile = new File("configuration.yaml");

    public YamlReader() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public Config readConfiguration() {
        try {
            return objectMapper.readValue(
                    configurationFile,
                    Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new NullPointerException("Unable to get Account Config");
    }
}