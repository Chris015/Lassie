package tag;

import com.amazonaws.services.ec2.AmazonEC2;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Owner implements Tag {

    private String name = "Owner";
    private AmazonEC2 ec2;

    public Owner() {

    }

    public Owner(AmazonEC2 ec2) {
        this.ec2 = ec2;
    }

    @Override
    public void tagEvent(String instanceId) {
        throw new NotImplementedException();
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
