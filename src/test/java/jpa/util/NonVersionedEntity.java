package jpa.util;

import jpa.util.Identity;

import javax.persistence.Entity;

@Entity
public class NonVersionedEntity extends Identity {

    public NonVersionedEntity() {
    }

    public NonVersionedEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "NonVersionedEntity{" +
                "name='" + name + '\'' +
                "} " + super.toString();
    }
}