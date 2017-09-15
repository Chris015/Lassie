package lassie;

import org.apache.log4j.Logger;

public class Main {
    private static Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Application is starting");
        new Application().run(args);
    }
}
