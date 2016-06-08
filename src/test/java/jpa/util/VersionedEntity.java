package jpa.util;

import jpa.util.Identity;

import javax.persistence.Entity;
import javax.persistence.Version;

@Entity
public class VersionedEntity extends Identity {

    @Version
    private int version;

    public VersionedEntity() {
    }

    public VersionedEntity(String name) {
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "VersionedEntity{" +
                "version=" + version +
                ", name='" + name + '\'' +
                "} " + super.toString();
    }
}