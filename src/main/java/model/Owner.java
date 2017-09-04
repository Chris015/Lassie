package model;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Owner implements Tag {

    private String name;

    public Owner() {
    }

    public Owner(String name) {
        this.name = name;
    }

    @Override
    public void tagEvent(String instanceId) {
        throw new NotImplementedException();
    }

    @Override
    public String getName() {
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }
}
