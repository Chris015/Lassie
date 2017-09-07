package lassie;

import lassie.config.Account;

public class Log {
    private Account account;
    private String filePath;

    public Log(Account account, String filePath) {
        this.account = account;
        this.filePath = filePath;
    }

    public Account getAccount() {
        return account;
    }

    public String getFilePath() {
        return filePath;
    }
}
