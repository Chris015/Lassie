import config.AccountConfig;
import config.ConfigReader;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        ConfigReader configReader = new ConfigReader();
        List<AccountConfig> accounts = configReader.readConfig();
        for (AccountConfig account : accounts) {
            System.out.println(account.getAccessKeyId());
        }


    }
}
