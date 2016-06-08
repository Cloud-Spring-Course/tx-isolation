package jpa.util;

import jpa.util.Identity;

import javax.persistence.Entity;

@Entity
public class TestEntity extends Identity {

    public TestEntity() {
    }

    public TestEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TestEntity{}";
    }
}