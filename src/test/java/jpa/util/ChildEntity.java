package jpa.util;

import javax.persistence.Entity;

@Entity
public class ChildEntity extends ParentEntity {

    public ChildEntity() {
    }

    public ChildEntity(String parentNameA) {
        this.parentNameA = parentNameA;
    }

    protected String parentNameA;

    public String getParentNameA() {
        return parentNameA;
    }

    public void setParentNameA(String parentNameA) {
        this.parentNameA = parentNameA;
    }
}
