package jdbc;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestJDBCMySQLConcurrentAnomalies {

    private Connection c1, c2;

    /**
     * Not supported in MySQL, can not create connection with isolation NONE
     */
    @Test
    public void testNone() throws Exception {
        try {
            setUpConnectionsAndCleanData(Connection.TRANSACTION_NONE);
        } catch (SQLException e) {
            Assert.assertEquals("Transaction isolation level NONE not supported by MySQL", e.getMessage());
        }
    }

    /**
     * TRANSACTION_READ_UNCOMMITTED
     *      transactions can read dirty data
     */
    @Test
    public void testReadUncommitted() throws Exception {
        int level = Connection.TRANSACTION_READ_UNCOMMITTED;
        setUpConnectionsAndCleanData(level);
        Assert.assertTrue(canReadDirtyData());

        setUpConnectionsAndCleanData(level);
        Assert.assertTrue(canReadNotRepeatable());

        setUpConnectionsAndCleanData(level);
        Assert.assertTrue(canReadPhantom());
    }

    /**
     * TRANSACTION_READ_COMMITTED
     *      not allows dirty data read
     *      allows not repeatable read
     */
    @Test
    public void testReadCommitted() throws Exception {
        int level = Connection.TRANSACTION_READ_COMMITTED;
        setUpConnectionsAndCleanData(level);
        Assert.assertFalse(canReadDirtyData());

        setUpConnectionsAndCleanData(level);
        Assert.assertTrue(canReadNotRepeatable());

        setUpConnectionsAndCleanData(level);
        Assert.assertTrue(canReadPhantom());
    }

    /**
     * TRANSACTION_REPEATABLE_READ
     *      not allows not repeatable reads, phantom inserts
     *      not allows dirty data read
     */
    @Test
    public void testRepeatableRead() throws Exception {
        int level = Connection.TRANSACTION_REPEATABLE_READ;
        setUpConnectionsAndCleanData(level); // TODO move inside methods
        Assert.assertFalse(canReadDirtyData());
        // in MySQL: if transaction read some data, for next reads it will get same snapshot of data
        // that's why phantom insert is not reproduced
        setUpConnectionsAndCleanData(level);
        Assert.assertFalse(canReadNotRepeatable());

        setUpConnectionsAndCleanData(level);
        Assert.assertFalse(canReadPhantom());
    }

    @Test
    public void testDefaultIsolationLevel() throws Exception {
        setUpConnectionsAndCleanData();
        Assert.assertFalse(canReadDirtyData());
        // in MySQL: if transaction read some data, for next reads it will get same snapshot of data
        // that's why phantom insert is not reproduced
        setUpConnectionsAndCleanData();
        Assert.assertFalse(canReadNotRepeatable());

        setUpConnectionsAndCleanData();
        Assert.assertFalse(canReadPhantom());
    }

    /**
     * TRANSACTION_SERIALIZABLE
     *      strict and full transaction isolation, like they work successively
     *
     *  In MySQL means the same as REPEATABLE_READ, but more strict.
     *  SERIALIZABLE enforces even stricter rules than REPEATABLE READ, and is used mainly in
     *  specialized situations, such as with XA transactions and for troubleshooting issues
     *  with concurrency and deadlocks.
     */
    @Test
    public void testSerializable() throws Exception {
        int level = Connection.TRANSACTION_SERIALIZABLE;

        long time = System.currentTimeMillis();
        try {
            setUpConnectionsAndCleanData(level);
            canReadDirtyData();
            Assert.fail("No lock timeout exceeded");
        } catch (SQLException e) {
            Assert.assertEquals("Lock wait timeout exceeded; try restarting transaction", e.getMessage());
            System.out.println("Lock timeout is " + (System.currentTimeMillis() - time));
            e.printStackTrace();
        }

        time = System.currentTimeMillis();
        try {
            setUpConnectionsAndCleanData(level);
            canReadNotRepeatable();
            Assert.fail("No lock timeout exceeded");
        } catch (SQLException e) {
            Assert.assertEquals("Lock wait timeout exceeded; try restarting transaction", e.getMessage());
            System.out.println("Lock timeout is " + (System.currentTimeMillis() - time));
            e.printStackTrace();
        }

        time = System.currentTimeMillis();
        try {
            setUpConnectionsAndCleanData(level);
            canReadPhantom();
            Assert.fail("No lock timeout exceeded");
        } catch (SQLException e) {
            Assert.assertEquals("Lock wait timeout exceeded; try restarting transaction", e.getMessage());
            System.out.println("Lock timeout is " + (System.currentTimeMillis() - time));
            e.printStackTrace();
        }
    }

    private boolean canReadDirtyData() throws Exception {
        // tx1 changes data, no commits jet
        c1.createStatement().executeUpdate("UPDATE TestEntity SET name='updated'");

        // tx2 can see changed and not committed data
        ResultSet tx2Entities = c2.createStatement().executeQuery("SELECT * FROM TestEntity WHERE name='updated'");
        return getNames(tx2Entities).contains("updated");
    }

    private boolean canReadNotRepeatable() throws Exception {
        // Transaction 1 reads data
        ResultSet tx1Data = c1.createStatement().executeQuery("SELECT * FROM TestEntity");
        String nameBeforeUpdate = getNames(tx1Data).get(0);

        // Transaction 2 modifies data
        c2.createStatement().executeUpdate("UPDATE TestEntity SET name='updated'");
        c2.commit();

        ResultSet tx1Data2 = c1.createStatement().executeQuery("SELECT * FROM TestEntity");
        String nameAfterUpdate = getNames(tx1Data2).get(0);

        return nameBeforeUpdate.equals("entity") && nameAfterUpdate.equals("updated");
    }

    private boolean canReadPhantom() throws Exception {
        ResultSet tx1Data = c1.createStatement().executeQuery("SELECT * FROM TestEntity");
        List<String> tx1Names = getNames(tx1Data);

        c2.createStatement().executeUpdate("INSERT INTO TestEntity (name) VALUES ('new entity2')");
        c2.createStatement().executeUpdate("INSERT INTO TestEntity (name) VALUES ('new entity3')");
        c2.commit();

        ResultSet tx1Data2 = c1.createStatement().executeQuery("SELECT * FROM TestEntity");
        List<String> tx1Names2 = getNames(tx1Data2);

        return tx1Names.size() != tx1Names2.size();
    }

    @After
    public void tearDown() throws Exception {
        rollbackAndClose(c1, c2);
    }

    public List<String> getNames(ResultSet rs) throws SQLException {
        List<String> result = new ArrayList<>();
        while (rs.next()) {
            result.add(rs.getString("name"));
        }

        return result;
    }

    private void setUpConnectionsAndCleanData() throws Exception {
        rollbackAndClose(c1, c2);
        prepareDatabase();
        c1 = createConnection();
        c2 = createConnection();
    }

    private void setUpConnectionsAndCleanData(int c1IsolationLevel, int c2IsolationLevel) throws Exception {
        setUpConnectionsAndCleanData();
        c1.setTransactionIsolation(c1IsolationLevel);
        c2.setTransactionIsolation(c2IsolationLevel);
    }

    private void setUpConnectionsAndCleanData(int isolationLevel) throws Exception {
        setUpConnectionsAndCleanData(isolationLevel, isolationLevel);
    }

    private void prepareDatabase() throws Exception {
        Connection c3 = createConnection();
        Statement statement = c3.createStatement();
        statement.execute("DELETE FROM TestEntity");
        statement.execute("INSERT INTO TestEntity (name) VALUES ('entity')");
        c3.commit();
        c3.close();
    }

    private Connection createConnection() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection c = DriverManager.getConnection("jdbc:mysql://localhost/testjpa?user=root&password=root");
        c.setAutoCommit(false);
        return c;
    }

    private void rollbackAndClose(Connection... connections) throws SQLException {
        for (Connection c : connections) {
            if (c != null) {
                c.rollback();
                c.close();
            }
        }
    }
}