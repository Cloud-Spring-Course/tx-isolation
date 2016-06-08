package jpa.util;

import javax.persistence.Entity;

@Entity
public class ParentEntity extends ChildEntity {

    public ParentEntity() {
    }

    public ParentEntity(String parentNameA) {
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
