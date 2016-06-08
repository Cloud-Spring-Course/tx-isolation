package jpa.util;

import jpa.transaction.model.NonVersionedEntityWithElementCollection;
import org.junit.After;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseJPATest {

    public static final String MYSQL = "testJpaMySQL";

    protected EntityManagerFactory emf;

    protected void setUp(Map<String, String> settings) {
        emf = Persistence.createEntityManagerFactory(MYSQL, settings);
    }

    protected void setUp(int txIsolationLevel) {
        Map<String, String> settings = new HashMap<>();
        settings.put("hibernate.connection.isolation", txIsolationLevel + "");
        setUp(settings);
    }

    protected void clearDB() {
        // Delete old data
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            // TODO iterate over all entities
            em.createQuery("DELETE FROM NonVersionedEntity").executeUpdate();
            em.createQuery("DELETE FROM VersionedEntity").executeUpdate();

            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    protected <T extends Identity> T store(T entity) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        em.close();
        return entity;
    }

    protected void close(EntityManager... ems) {
        for (EntityManager em : ems) {
            em.close();
        }
    }

    @After
    public void tearDown() {
        emf.close();
    }
}
