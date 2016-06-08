package jpa;

import jpa.util.BaseJPATest;
import jpa.util.ParentEntity;
import jpa.util.ChildEntity;
import jpa.util.TestEntity;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Connection;

public class TestJPAPessimisticLocking extends BaseJPATest {

    protected EntityManager em1;
    protected EntityManager em2;

    @Before
    public void before() {
        setUp(Connection.TRANSACTION_READ_COMMITTED);
        em1 = emf.createEntityManager();
        em2 = emf.createEntityManager();
    }

    @Test
    public void testPessimisticReadLocking() {
        TestEntity entity = store(new TestEntity("entity"));
        TestEntity entity2 = store(new TestEntity("entity2"));

        em1.getTransaction().begin();
        em2.getTransaction().begin();

        // We only got data, haven't block it already
        TestEntity em1Entity = em1.find(TestEntity.class, entity.getId());
        // Lock object, separate SELECT ... LOCK FOR SHARE MODE is executed
        em1.lock(em1Entity, LockModeType.PESSIMISTIC_READ);

        // Read second entity with locking
        em1.find(TestEntity.class, entity2.getId(), LockModeType.PESSIMISTIC_READ);

        // Try to read and modify it in em2
        TestEntity em2Entity = em2.find(TestEntity.class, entity.getId(), LockModeType.PESSIMISTIC_READ);
        em2Entity.setName("changed by em2");

        try {
            // Commit fails on timeout because objects are locked by another transaction
            em2.getTransaction().commit();
        } catch (RollbackException e) {
            Assert.assertTrue(e.getCause() instanceof LockTimeoutException);
            e.printStackTrace();
        }
    }

    @Test
    public void testPessimisticWriteLocking() {
        TestEntity entity = store(new TestEntity("entity"));

        em1.getTransaction().begin();
        em2.getTransaction().begin();

        // We only got data, haven't block it already
        TestEntity em1Entity = em1.find(TestEntity.class, entity.getId());
        // Lock object, separate SELECT ... LOCK FOR SHARE MODE is executed
        em1.lock(em1Entity, LockModeType.PESSIMISTIC_WRITE);

        try {
            // Try to read same entity in different transaction
            em2.find(entity.getClass(), entity.getId(), LockModeType.PESSIMISTIC_WRITE);
            Assert.fail("No timeout exception");
        } catch (LockTimeoutException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPessimisticLockingInheritance() {
        ChildEntity entity = store(new ChildEntity("entity"));

        em1.getTransaction().begin();
        em2.getTransaction().begin();

        // Lock child entity
        em1.find(ChildEntity.class, entity.getId(), LockModeType.PESSIMISTIC_WRITE);

        try {
            // Try to read same entity in different transaction
            em2.find(ParentEntity.class, entity.getId(), LockModeType.PESSIMISTIC_WRITE);
            Assert.fail("No exception");
        } catch (LockTimeoutException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        em1.close();
        em2.close();
    }
}