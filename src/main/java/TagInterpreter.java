import com.amazonaws.services.ec2.AmazonEC2;
import exceptions.UnsupportedTagException;
import model.Owner;
import model.Tag;


public class TagInterpreter {

    AmazonEC2 ec2;

    public TagInterpreter(AmazonEC2 ec2) {
        this.ec2 = ec2;
    }

    public Tag interpret(String tag) throws UnsupportedTagException {

        switch (tag) {
            case "Owner": {
                return new Owner(ec2);
            }
            default: {
                throw new UnsupportedTagException(tag + " is not supported");
            }
        }

    }
}

