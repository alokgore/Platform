package com.tejas.config;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.tejas.config.ApplicationConfig;


import junit.framework.TestCase;

public class ApplicationConfigTest extends TestCase
{
	private static final String DOMAIN = "debug";
	private static final String APP = "cms_merge";
	private static final String APPGROUP = "cms";
	private static final String TMP_DIR = "/tmp/config_temp/";
	private File configBaseDir;

	@Override
	protected void setUp() throws Exception
	{
		setupConfigTree();
		writeBaseConfig();
	}

	private void writeBaseConfig() throws IOException
	{
		writeToFile("global/global.cfg", "*.global_key.a.b.c=\"String value\";");
		writeToFile("appgroup/" + APPGROUP + ".cfg", "*.appgroup_key.a.b.c=10;");
		writeToFile("app/" + APP + ".cfg", "debug.app_key.a.b.c=10.5;");
	}

	private void setupConfigTree()
	{
		configBaseDir = new File(TMP_DIR + File.separator + randomAlphabetic(5).toLowerCase());
		System.out.println("Using [" + configBaseDir.getAbsolutePath() + "] as the Configuration base dir");

		String baseFolders[] = new String[] { "global", "appgroup", "app", "override" };
		for (String baseFolder : baseFolders)
		{
			File baseDir = new File(configBaseDir.getAbsolutePath() + File.separator + baseFolder);
			if (baseDir.mkdirs() == false)
			{
				throw new RuntimeException("Failed to create the config base dir " + baseDir.getAbsolutePath());
			}
		}
	}

	public void testSanity() throws Exception
	{
		System.out.println("Testing sanity");
		initAppConfig();
	}

	private void initAppConfig()
	{
		System.out.println(" ************************************ Initializing ApplicationConfig************************************ ");
		ApplicationConfig.initialize(configBaseDir.getAbsolutePath(), DOMAIN, APPGROUP, APP, "cms_merge.cfg", "ingrams.cfg");
		System.out.println(ApplicationConfig.dumpConfig());
	}

	/**
	 * Same key twice in a file. Latest occurrence should be picked up
	 */
	public void testDuplicateKeysSameFile() throws Exception
	{
		writeToFile("global/global.cfg", "*.key=value1;");
		writeToFile("global/global.cfg", "*.key=value2;");
		initAppConfig();

		assertEquals("value2", ApplicationConfig.findString("key"));
	}

	/**
	 * Same key in global and appgroup folders. Appgroup key should override global
	 */
	public void testAppgroupOverGlobal() throws Exception
	{
		writeToFile("appgroup/" + APPGROUP + ".cfg", "*.key=appgroup_value;");
		writeToFile("global/global.cfg", "*.key=global_value;");

		initAppConfig();

		assertEquals("appgroup_value", ApplicationConfig.findString("key"));
	}

	/**
	 * Same key in app and appgroup folders. App key should override appgrop
	 */
	public void testAppOverAppgroup() throws Exception
	{
		writeToFile("appgroup/" + APPGROUP + ".cfg", "*.key=appgroup_value;");
		writeToFile("app/" + APP + ".cfg", "*.key=app_value;");

		initAppConfig();

		assertEquals("app_value", ApplicationConfig.findString("key"));
	}

	/**
	 * Entries bound to other domains should not be read
	 */
	public void testExcludeOtherDomains() throws Exception
	{
		writeToFile("global/global.cfg", "*.my_key=value;");
		writeToFile("app/" + APP + ".cfg", "my_new_domain.my_key=new_domain_value;");

		initAppConfig();

		assertEquals("value", ApplicationConfig.findString("my_key"));
	}

	/**
	 * Same key in with a domain specific entry and a star entry. Domain entry should override the
	 * star entry. Irrespective of the order of appearance.
	 */
	public void testDomainOverStar() throws Exception
	{
		writeToFile("global/global.cfg", "*.key1=star_key1_global;");
		writeToFile("app/" + APP + ".cfg", DOMAIN + ".key1=domain_key1_app;");

		writeToFile("global/global.cfg", DOMAIN + ".key2=domain_key2_global;");
		writeToFile("app/" + APP + ".cfg", "*.key2=star_key2_app;");

		initAppConfig();

		assertEquals("domain_key1_app", ApplicationConfig.findString("key1"));
		assertEquals("domain_key2_global", ApplicationConfig.findString("key2"));
	}

	/**
	 * Keys inserted using the insertXXX methods on ApplicationConfig override everything else.
	 */
	public void testInsertKeys() throws Exception
	{
		writeToFile("app/" + APP + ".cfg", DOMAIN + ".key=1;");
		initAppConfig();
		assertEquals(1, ApplicationConfig.findInteger("key").intValue());
		ApplicationConfig.insertInteger("key", 2);
		assertEquals(2, ApplicationConfig.findInteger("key").intValue());

		System.out.println(ApplicationConfig.dumpConfig());
	}

	public void testList() throws Exception
	{
		writeToFile("app/" + APP + ".cfg", DOMAIN + ".key=(\"one\", \"two\", \"three\");");
		initAppConfig();

		List<Object> list = ApplicationConfig.findList("key");
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEquals("one", list.get(0));
		assertEquals("two", list.get(1));
		assertEquals("three", list.get(2));
	}

	public void testMap() throws Exception
	{
		writeToFile("app/" + APP + ".cfg", DOMAIN + ".key={k1=v1; k2=v2;};");
		initAppConfig();

		Map<String, Object> map = ApplicationConfig.findMap("key");

		assertNotNull(map);
		assertEquals(2, map.size());

		assertEquals("v1", map.get("k1"));
		assertEquals("v2", map.get("k2"));
	}

	public void testFindByPrefix() throws Exception
	{
		writeToFile("global/global.cfg", "*.prefix.a.b1.key1=v1;");
		writeToFile("global/global.cfg", "*.prefix.a.b2.key2=v2;");
		writeToFile("global/global.cfg", "*.prefix.a.b3.key3=v3;");

		writeToFile("global/global.cfg", "*.prefix.b.key1=v4;");
		writeToFile("global/global.cfg", "*.prefix.b.key2=v4;");

		initAppConfig();

		Map<String, Object> objectsByPrefix = ApplicationConfig.findObjectsByPrefix("prefix");
		System.out.println(objectsByPrefix);
		assertEquals(5, objectsByPrefix.size());
		assertEquals("v1", objectsByPrefix.get("prefix.a.b1.key1"));

		Map<String, Object> objectsByPrefixA = ApplicationConfig.findObjectsByPrefix("prefix.a");
		System.out.println(objectsByPrefixA);
		assertEquals(3, objectsByPrefixA.size());
		assertEquals("v2", objectsByPrefix.get("prefix.a.b2.key2"));
	}

	private void writeToFile(String relativeFilePath, String... configLines) throws IOException
	{
		File file = new File(configBaseDir + File.separator + relativeFilePath);
		PrintWriter writer = new PrintWriter(new FileWriter(file, true));
		for (String configLine : configLines)
		{
			writer.println(configLine);
		}
		writer.close();
	}

	@Override
	protected void tearDown() throws Exception
	{
		ApplicationConfig.destroy();
	}
}
