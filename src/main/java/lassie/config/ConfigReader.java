package lassie.config;

import java.util.List;

public interface ConfigReader {

    List<Account> getAccounts();

    boolean getDryRun();

}
