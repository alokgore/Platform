package com.tejas.chanak.types;

import static com.tejas.core.enums.DatabaseEndpoints.LOCAL_MYSQL;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tejas.chanak.console.DAGManagerConsoleUtils;
import com.tejas.chanak.core.DAGManager;
import com.tejas.chanak.monitoring.DAGMonitor;
import com.tejas.chanak.types.ContractConfiguration.SelectKey;
import com.tejas.chanak.types.LogEntry.Mapper;
import com.tejas.chanak.types.orm.DAGDetails;
import com.tejas.chanak.types.orm.DAGSummary;
import com.tejas.config.ApplicationConfig;
import com.tejas.core.TejasContext;
import com.tejas.dbl.MySQLEndpoint;
import com.tejas.dbl.TejasDBLRegistry;
import com.tejas.utils.misc.DBLTestUtils;

public class DatabaseMappingTest
{
    
    @BeforeClass
    public static void setupClass() throws Exception
    {
        ApplicationConfig.initialize(null, "test", "platform", "tejas-test");
        TejasDBLRegistry.registerEndpoint(new MySQLEndpoint.Builder(LOCAL_MYSQL).withDatabaseName("platform").build(), true);
    }
    
    @AfterClass
    public static void tearDown() throws Exception
    {
        TejasDBLRegistry.shutdown();
    }
    
    private String dagID;
    private String contractID;
    
    private TejasContext self;
    
    @Before
    public void setup()
    {
        this.self = new TejasContext();
        this.dagID = RandomStringUtils.randomAlphanumeric(10);
        this.contractID = RandomStringUtils.randomAlphanumeric(10);
    }
    
    @Test
    public void testContractConfigMapping()
    {
        com.tejas.chanak.types.ContractConfiguration.Mapper mapper = this.self.dbl.getMybatisMapper(ContractConfiguration.Mapper.class);
        
        assertEquals(0, mapper.countConfig(this.contractID));
        ContractConfiguration config1 = new ContractConfiguration(this.dagID, this.contractID, "my_key", "my_value");
        mapper.insert(config1);
        assertEquals(1, mapper.countConfig(this.contractID));
        assertEquals(config1, mapper.getConfigForContract(this.contractID).get(0));
        
        ContractConfiguration config2 = new ContractConfiguration(this.dagID, this.contractID, "my_key : 2", "my_value : 2");
        ContractConfiguration config3 = new ContractConfiguration(this.dagID, this.contractID, "my_key", "Return of the 'my_value'");
        mapper.insert(config2);
        mapper.insert(config3);
        assertEquals(3, mapper.countConfig(this.contractID));
        
        assertEquals(2, mapper.getConfigForKey(new SelectKey(this.contractID, "my_key")).size());
        assertEquals(1, mapper.getConfigForKey(new SelectKey(this.contractID, "my_key : 2")).size());
    }
    
    @Test
    public void testDependencyGraph()
    {
        com.tejas.chanak.types.DependencyGraph.Mapper mapper = this.self.dbl.getMybatisMapper(DependencyGraph.Mapper.class);
        mapper.insert(new DAGSummary(this.dagID, "Desc of " + this.dagID));
        assertEquals(0, mapper.countUnfinishedContracts(this.dagID));
        mapper.resumeDAG(this.dagID);
        mapper.suspendDAG(this.dagID);
    }
    
    @Test
    public void testEmptyResults() throws Exception
    {
        List<DAGDetails> dagDetails = DAGManagerConsoleUtils.getDAGDetails(new TejasContext());
        System.err.println("DAG-DETAILS ARE " + dagDetails);
    }
    
    @Test
    public void testLogEntryMapping()
    {
        Mapper mapper = this.self.dbl.getMybatisMapper(LogEntry.Mapper.class);
        assertEquals(0, mapper.countDAGLog(this.dagID));
        LogEntry entry = new LogEntry(this.dagID, this.contractID, new ContractExecutionReport(""));
        mapper.insertLog(entry);
        assertEquals(1, mapper.countDAGLog(this.dagID));
        Assert.assertEquals(entry, mapper.getLogEntries(this.contractID).get(0));
        mapper.deleteDAGLog(this.dagID);
        assertEquals(0, mapper.countDAGLog(this.dagID));
    }
    
    @Test
    public void testMapperSanity() throws Exception
    {
        DBLTestUtils.testMapper(DependencyGraph.Mapper.class);
        DBLTestUtils.testMapper(DAGManagerConsoleUtils.Mapper.class);
        DBLTestUtils.testMapper(DAGManager.Mapper.class);
        DBLTestUtils.testMapper(DAGMonitor.Mapper.class);
        DBLTestUtils.testMapper(ContractConfiguration.Mapper.class);
        DBLTestUtils.testMapper(DAGContract.Mapper.class, true);
        DBLTestUtils.testMapper(Dependency.Mapper.class);
        DBLTestUtils.testMapper(LogEntry.Mapper.class);
    }
    
}
