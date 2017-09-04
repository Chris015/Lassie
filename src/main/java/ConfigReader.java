import com.amazonaws.services.ec2.model.Tag;
import model.Account;
import model.AccountsConfiguration;
import model.Event;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;

public class ConfigReader {

    private AccountsConfiguration accounts;

    private Constructor constructor;
    private TypeDescription typeDescription;
    private Yaml yaml;


    public AccountsConfiguration readConfig() {

        constructor = new Constructor(AccountsConfiguration.class);
        typeDescription = new TypeDescription(AccountsConfiguration.class);
        typeDescription.putListPropertyType("accounts", Account.class);
        constructor.addTypeDescription(typeDescription);

        typeDescription = new TypeDescription(Account.class);
        typeDescription.putListPropertyType("events", Event.class);
        constructor.addTypeDescription(typeDescription);

        typeDescription = new TypeDescription(Event.class);
        typeDescription.putListPropertyType("tags", Tag.class);
        constructor.addTypeDescription(typeDescription);

        yaml = new Yaml(constructor);
        try (InputStream in = ClassLoader.getSystemResourceAsStream("configuration.yaml")) {
            accounts = yaml.loadAs(in, AccountsConfiguration.class);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return accounts;
    }
}

