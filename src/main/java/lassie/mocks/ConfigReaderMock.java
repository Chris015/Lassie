package lassie.mocks;

import lassie.config.Config;
import lassie.config.ConfigReader;

public class ConfigReaderMock implements ConfigReader {
    public static Config config = new Config();

    @Override
    public Config readConfiguration() {
        return config;
    }
}
