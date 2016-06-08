package jpa;

import jpa.util.NonVersionedEntity;
import jpa.util.VersionedEntity;
import jpa.util.BaseJPATest;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;
import java.sql.Connection;

public class TestJPAOptimisticLocking extends BaseJPATest {

    protected EntityManager em1;
    protected EntityManager em2;
    protected EntityManager em3;

    @Before
    public void before() {
        setUp(Connection.TRANSACTION_READ_COMMITTED);
        clearDB();
        store(new VersionedEntity("initial name"));
        store(new NonVersionedEntity("initial name"));

        em1 = emf.createEntityManager();
        em2 = emf.createEntityManager();
        em3 = emf.createEntityManager();
    }

    /**
     * Entity does not have @Version column, we do not have OptimisticLockException
     */
    @Test
    public void testNonVersionedEntityNoOptimisticLock() {
        em1.getTransaction().begin();
        em2.getTransaction().begin();

        NonVersionedEntity e1 = em1.createQuery("from NonVersionedEntity", NonVersionedEntity.class).getSingleResult();
        e1.setName("tx1 name");

        NonVersionedEntity e2 = em2.createQuery("from NonVersionedEntity", NonVersionedEntity.class).getSingleResult();
        e2.setName("tx2 name");

        em1.getTransaction().commit();
        em2.getTransaction().commit();

        // Check what is in database
        NonVersionedEntity e3 = em3.createQuery("from NonVersionedEntity", NonVersionedEntity.class).getSingleResult();
        Assert.assertEquals("tx2 name", e3.getName());
    }

    @Test
    public void testVersionedEntityOptimisticLock() {
        em1.getTransaction().begin();
        em2.getTransaction().begin();

        VersionedEntity e1 = em1.createQuery("from VersionedEntity", VersionedEntity.class).getSingleResult();
        Assert.assertEquals(0, e1.getVersion());
        e1.setName("tx1 name");

        VersionedEntity e2 = em2.createQuery("from VersionedEntity", VersionedEntity.class).getSingleResult();
        Assert.assertEquals(0, e2.getVersion());
        e2.setName("tx2 name");

        em1.getTransaction().commit();

        try {
            em2.getTransaction().commit();
        } catch (RollbackException e) {
            Assert.assertTrue(e.getCause() instanceof OptimisticLockException);
            e.printStackTrace();
        }

        // Check what is in database
        VersionedEntity e3 = em3.createQuery("from VersionedEntity", VersionedEntity.class).getSingleResult();
        Assert.assertEquals("tx1 name", e3.getName());
        Assert.assertEquals(1, e3.getVersion());
    }

    /**
     * When execution of bulk UPDATE - JPA optimistic locking doesn't work
     */
    @Test
    public void testOptimisticLockingDoNotWorkWhenBulkUpdate() {
        em1.getTransaction().begin();
        em1.createQuery("UPDATE VersionedEntity ve SET ve.name='changed in bulk'").executeUpdate();
        em1.getTransaction().commit();

        VersionedEntity entity = em2.createQuery("from VersionedEntity", VersionedEntity.class).getSingleResult();
        // Entity name was changed
        Assert.assertEquals("changed in bulk", entity.getName());
        // Entity version remain not changed
        Assert.assertEquals(0, entity.getVersion());
    }

    @After
    public void tearDown() {
        close(em1, em2, em3);
    }
}