package lassie;

import com.amazonaws.services.ec2.AmazonEC2;
import lassie.exception.UnsupportedTagException;
import lassie.tag.Owner;
import lassie.tag.Tag;


public class TagInterpreter {

    private AmazonEC2 ec2;
    private LogPersister logPersister;

    public TagInterpreter(AmazonEC2 ec2, LogPersister logPersister) {
        this.ec2 = ec2;
        this.logPersister = logPersister;
    }

    public Tag interpret(String tag) throws UnsupportedTagException {

        switch (tag) {
            case "Owner": {
                return new Owner(ec2, logPersister);
            }
            default: {
                throw new UnsupportedTagException(tag + " is not supported");
            }
        }

    }
}

