package com.tejas.dbl;

import static com.tejas.dbl.DatabaseEndpoint.EndpointType.READ_ONLY;
import static com.tejas.dbl.FoxDBLayerTest.Endpoints.MYSQL_MASTER;
import static com.tejas.dbl.FoxDBLayerTest.Endpoints.MYSQL_SLAVE;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasDBLayer;
import com.tejas.dbl.MySQLEndpoint;
import com.tejas.dbl.TejasDBLRegistry;
import com.tejas.dbl.TejasDBLayerImpl;
import com.tejas.dbl.DatabaseEndpoint.DatabaseVendor;
import com.tejas.dbl.DatabaseEndpoint.EndpointType;
import com.tejas.types.exceptions.AccessControlException;
import com.tejas.utils.misc.StringUtils;


public class FoxDBLayerTest
{
    private static class AccessControlTester
    {
        public static <T> void execute(Callable<T> callable, boolean writesAllowed)
        {
            try
            {
                callable.call();
                Assert.assertTrue("An attempt to run a DML/DDL should have failed on a Read-Only DBLayer", writesAllowed);
            }
            catch (Exception e)
            {
                Assert.assertTrue(StringUtils.serializeToString(e).contains(AccessControlException.class.getCanonicalName()));
                Assert.assertTrue("An attempt to run a DML/DDL should have succeeded on a Read-Write DBLayer", writesAllowed == false);
            }
        }
    }

    public static enum Endpoints
    {
            MYSQL_MASTER,
            MYSQL_SLAVE;
    }

    @AfterClass
    public static void cleanup() throws Exception
    {
        TejasDBLRegistry.shutdown();
    }

    @BeforeClass
    public static void setup() throws Exception
    {
        ApplicationConfig.initialize("", "", "platform", "platform-test");
        TejasDBLRegistry.registerEndpoint(new MySQLEndpoint.Builder(MYSQL_MASTER).withDatabaseName("platform").build());
        TejasDBLRegistry.registerEndpoint(new MySQLEndpoint.Builder(MYSQL_SLAVE).withType(READ_ONLY).withDatabaseName("platform").build());
    }

    private List<Foo> fillTable(FooMapper mapper, int numRows)
    {
        System.out.println("Filling " + numRows + " rows in the FOO table");
        mapper.truncateTable();
        for (int i = 0; i < numRows; i++)
        {
            mapper.insertFoo(new Foo());
        }
        return mapper.selectAllFoos();
    }

    private List<Foo> fillTable(TejasDBLayer dbl, int numRows)
    {
        FooMapper mapper = dbl.getMybatisMapper(FooMapper.class);
        return fillTable(mapper, numRows);
    }

    private void testAccessControl(TejasDBLayer dbl, boolean writesAllowed)
    {
        final FooMapper mapper = dbl.getMybatisMapper(FooMapper.class);

        List<Foo> dataBeforeTest = mapper.selectAllFoos();

        Assert.assertTrue("FOO table needs to have some data before this test", dataBeforeTest.size() > 0);

        // Should always be allowed
        mapper.countEntries();

        // Should always be allowed
        mapper.selectAllFoos();

        AccessControlTester.execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception
            {
                mapper.createTable();
                return null;
            }
        }, writesAllowed);

        AccessControlTester.execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception
            {
                mapper.truncateTable();
                return null;
            }
        }, writesAllowed);

        AccessControlTester.execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception
            {
                mapper.deleteAllData();
                return null;
            }
        }, writesAllowed);

        AccessControlTester.execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception
            {
                mapper.dropTable();
                mapper.createTable();
                return null;
            }
        }, writesAllowed);

        List<Foo> dataAfterTest = mapper.selectAllFoos();

        if (writesAllowed)
        {
            Assert.assertTrue(dataBeforeTest.size() != dataAfterTest.size());
        }
        else
        {
            Assert.assertTrue(dataBeforeTest.size() == dataAfterTest.size());
        }
    }

    private void testBatchSelect(FooMapper fooMapper)
    {
        Map<String, Foo> map = new Hashtable<String, Foo>();
        for (int i = 0; i < 10; i++)
        {
            Foo foo = new Foo();
            map.put(foo.getFooString(), foo);
            fooMapper.insertFoo(foo);
            System.out.println("Inserted " + foo);
        }

        List<Foo> foos = fooMapper.selectAllFoos();
        for (Foo foo : foos)
        {
            System.out.println("Matching " + foo);
            foo.assertEquals(map.get(foo.getFooString()));
        }
    }

    private void testDelete(FooMapper mapper)
    {
        List<Foo> data = fillTable(mapper, 10);
        Foo foo = data.remove(0);

        Assert.assertNotNull(mapper.selectByFooId(foo.getFooId()));
        Assert.assertEquals(1, mapper.deleteFooById(foo.getFooId()));

        Assert.assertEquals(0, mapper.deleteFooById(foo.getFooId()));
        Assert.assertEquals(0, mapper.deleteFooByString(foo.getFooString()));

        Assert.assertEquals(1, mapper.deleteFooByString(data.remove(0).getFooString()));

        Assert.assertEquals(8, mapper.selectAllFoos().size());
    }

    private void testFooMapper(TejasDBLayer dbl) throws Exception
    {
        FooMapper mapper = dbl.getMybatisMapper(FooMapper.class);
        mapper.dropTable();
        mapper.createTable();
        mapper.deleteAllData();
        testSimpleFooInsertion(mapper);
        testBatchSelect(mapper);
        testSelectLimits(mapper);
        testDelete(mapper);
    }

    private void testReadCommited(TejasDBLayer dblOne, TejasDBLayer dblTwo)
    {
        fillTable(dblOne, 10);

        dblOne.startTransaction(TransactionIsolationLevel.READ_COMMITTED);
        dblTwo.startTransaction(TransactionIsolationLevel.READ_COMMITTED);

        FooMapper mapperOne = dblOne.getMybatisMapper(FooMapper.class);
        FooMapper mapperTwo = dblTwo.getMybatisMapper(FooMapper.class);

        mapperOne.insertFoo(new Foo());
        Assert.assertEquals(11, mapperOne.countEntries());
        Assert.assertEquals(10, mapperTwo.countEntries());

        dblOne.commit();
        Assert.assertEquals(11, mapperOne.countEntries());
        Assert.assertEquals(10, mapperTwo.countEntries());

        mapperTwo.insertFoo(new Foo());
        Assert.assertEquals(11, mapperOne.countEntries());
        Assert.assertEquals(12, mapperTwo.countEntries());

        dblTwo.commit();
        Assert.assertEquals(12, mapperOne.countEntries());
        Assert.assertEquals(12, mapperTwo.countEntries());
    }

    private void testReadUncommited(TejasDBLayer dblOne, TejasDBLayer dblTwo)
    {
        fillTable(dblOne, 10);

        dblOne.startTransaction(TransactionIsolationLevel.READ_UNCOMMITTED);
        dblTwo.startTransaction(TransactionIsolationLevel.READ_UNCOMMITTED);

        FooMapper mapperOne = dblOne.getMybatisMapper(FooMapper.class);
        FooMapper mapperTwo = dblTwo.getMybatisMapper(FooMapper.class);

        mapperOne.insertFoo(new Foo());
        Assert.assertEquals(11, mapperOne.countEntries());
        Assert.assertEquals(11, mapperTwo.countEntries());

        dblOne.commit();
        Assert.assertEquals(11, mapperOne.countEntries());
        Assert.assertEquals(11, mapperTwo.countEntries());

        mapperTwo.insertFoo(new Foo());
        Assert.assertEquals(11, mapperOne.countEntries());
        Assert.assertEquals(12, mapperTwo.countEntries());

        dblTwo.commit();
        Assert.assertEquals(12, mapperOne.countEntries());
        Assert.assertEquals(12, mapperTwo.countEntries());
    }

    private void testSelectLimits(FooMapper mapper)
    {
        mapper.truncateTable();
        Assert.assertEquals(0, mapper.countEntries());

        System.out.println("Inserting 100 Foos");

        for (int i = 0; i < 100; i++)
        {
            mapper.insertFoo(new Foo());
        }
        Assert.assertEquals(100, mapper.countEntries());

        Assert.assertEquals(10, mapper.selectSomeFoos(new RowBounds(0, 10)).size());
        Assert.assertEquals(20, mapper.selectSomeFoos(new RowBounds(10, 20)).size());
        Assert.assertEquals(40, mapper.selectSomeFoos(new RowBounds(50, 40)).size());
        Assert.assertEquals(10, mapper.selectSomeFoos(new RowBounds(90, 40)).size());
    }

    private void testSimpleFooInsertion(FooMapper mapper)
    {
        System.out.println("Truncate : " + mapper.truncateTable());
        Assert.assertEquals(0, mapper.countEntries());

        Foo foo = new Foo();
        long idBeforeInsertion = foo.getFooId();

        System.out.println("Insert : " + mapper.insertFoo(foo));
        long idAfterInsertion = foo.getFooId();
        Assert.assertNotSame(idAfterInsertion, idBeforeInsertion);
        System.out.println(foo);

        Foo fooFromDB = mapper.selectByFooString(foo.getFooString());
        System.out.println(fooFromDB);
        foo.assertEquals(fooFromDB);

        Assert.assertEquals(1, mapper.countEntries());
        mapper.truncateTable();
        Assert.assertEquals(0, mapper.countEntries());
    }

    @Test
    public void testAccessControl() throws Exception
    {
        TejasDBLayer masterDBL = TejasDBLRegistry.getDBLayer(MYSQL_MASTER);
        TejasDBLayer slaveDBL = TejasDBLRegistry.getDBLayer(MYSQL_SLAVE);
        TejasDBLayer readOnlyMasterDBL = TejasDBLRegistry.getDBLayer(MYSQL_SLAVE, READ_ONLY);

        fillTable(masterDBL, 100);
        testAccessControl(masterDBL, true);

        fillTable(masterDBL, 100);
        testAccessControl(slaveDBL, false);

        testAccessControl(readOnlyMasterDBL, false);
    }

    @Test
    public void testBasicSetup() throws Exception
    {
        TejasDBLayerImpl rwDbl = (TejasDBLayerImpl) TejasDBLRegistry.getDBLayer(MYSQL_MASTER);
        Assert.assertEquals(false, rwDbl.isTransactional());
        Assert.assertEquals(false, rwDbl.isReadOnly());
        Assert.assertEquals(EndpointType.READ_WRITE, rwDbl.getEndpoint().type);
        Assert.assertEquals(DatabaseVendor.MySQL, rwDbl.getEndpoint().vendor);
        Assert.assertEquals(MYSQL_MASTER, rwDbl.getEndpoint().name);

        TejasDBLayerImpl roDbl = (TejasDBLayerImpl) TejasDBLRegistry.getDBLayer(MYSQL_MASTER, READ_ONLY);
        Assert.assertEquals(false, roDbl.isTransactional());
        Assert.assertEquals(true, roDbl.isReadOnly());
        Assert.assertEquals(EndpointType.READ_WRITE, roDbl.getEndpoint().type);
        Assert.assertEquals(DatabaseVendor.MySQL, roDbl.getEndpoint().vendor);
        Assert.assertEquals(MYSQL_MASTER, roDbl.getEndpoint().name);

        TejasDBLayerImpl slaveDbl = (TejasDBLayerImpl) TejasDBLRegistry.getDBLayer(MYSQL_SLAVE, READ_ONLY);
        Assert.assertEquals(false, slaveDbl.isTransactional());
        Assert.assertEquals(true, slaveDbl.isReadOnly());
        Assert.assertEquals(EndpointType.READ_ONLY, slaveDbl.getEndpoint().type);
        Assert.assertEquals(DatabaseVendor.MySQL, slaveDbl.getEndpoint().vendor);
        Assert.assertEquals(MYSQL_SLAVE, slaveDbl.getEndpoint().name);
    }

    @Test
    public void testMapper() throws Exception
    {
        TejasDBLayerImpl dbl = (TejasDBLayerImpl) TejasDBLRegistry.getDBLayer(MYSQL_MASTER);
        testFooMapper(dbl);
    }

    @Test
    public void testTransactions() throws Exception
    {
        testReadCommited(TejasDBLRegistry.getDBLayer(MYSQL_MASTER), TejasDBLRegistry.getDBLayer(MYSQL_MASTER));
        testReadUncommited(TejasDBLRegistry.getDBLayer(MYSQL_MASTER), TejasDBLRegistry.getDBLayer(MYSQL_MASTER));
    }
}
