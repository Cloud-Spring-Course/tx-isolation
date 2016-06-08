package jpa.util;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * MappedSuperclass is not Entity. It does not have own table. We can not query it.
 * All columns are included in Entity class tables.
 */
@MappedSuperclass
public class Identity implements Serializable {

    @Id
    @GeneratedValue
    protected Long id;

    protected String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Identity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}