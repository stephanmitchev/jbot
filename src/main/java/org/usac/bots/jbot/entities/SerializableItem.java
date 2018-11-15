package org.usac.bots.jbot.entities;

import java.io.Serializable;

public abstract class SerializableItem implements Serializable {

    protected String alias;
    protected String owner;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }


}
