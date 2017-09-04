import model.AccountsConfiguration;

public class Main {

    public static void main(String[] args) {

        AccountsConfiguration accounts = new AccountsConfiguration();

        ConfigReader configReader = new ConfigReader();
        accounts = configReader.readConfig();

        System.out.println(accounts.toString());


    }
}
