package com.tejas.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

class ConfigTree
{

	private static final String DEFAULT_CONFIG_ROOT = "/opt/tejas/config";

	static class ConfigFileFilter implements java.io.FilenameFilter
	{
        @Override
        public boolean accept(java.io.File dir, String filename)
		{
			return filename.endsWith(CONFIG_SUFFIX) && !filename.startsWith(".");
		}
	}

	private static final String APP = "app";
	private static final String APPGROUP = "appgroup";
	private final static String CONFIG_SUFFIX = ".cfg";

	private static final String GLOBAL = "global";

	private static final Logger logger = Logger.getLogger(ConfigTree.class);

	private static final String OVERRIDE = "override";

	/**
	 * The path constant for programmatic insertion.
	 */
	private final static String PROGRAMMATIC_INSERTION = "programmatic insertion";

	/**
	 * The AppGroup name.
	 */
	private final String appGroup;

	/**
	 * The Application name
	 */
	private final String appName;

	private final String configRoot;

	/**
	 * The application domain.
	 */
	private final String domain;

	/**
	 * The names of the override file(s) used for this tree
	 */
	private final List<String> overrides_;

	private BranchNode root_;

	public ConfigTree(String configurationRoot, String domainName, String applicationGroup, String applicationName, String... overrideFiles)
	{
		this.configRoot = getConfigurationRoot(configurationRoot);
		this.domain = getDomainName(this.configRoot, domainName);
		this.appName = applicationName;
		this.appGroup = applicationGroup;
		this.overrides_ = Arrays.asList(overrideFiles);

		readConfigFiles();
	}

	private String getConfigurationRoot(String configurationRoot)
	{
		if ((configurationRoot != null) && (configurationRoot.trim().equals("") == false))
		{
			return configurationRoot;
		}
		return DEFAULT_CONFIG_ROOT;
	}

	private String getDomainName(String configurationRoot, String domainName)
	{
		if ((domainName != null) && (domainName.trim().equals("") == false))
		{
			return domainName;
		}

		String deploymentDomain = "debug";
		String domainFileName = configurationRoot + "/domain";
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(domainFileName));

			String line = null;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.startsWith("#"))
				{
					continue;
				}
				deploymentDomain = line;
				break;
			}
		}
		catch (IOException exception)
		{
			System.out.println("Did not find the file [" + domainFileName + "]. Assuming domain " + deploymentDomain);
		}
		finally
		{
			IOUtils.closeQuietly(reader);
		}
		return deploymentDomain;
	}

	public Boolean findBoolean(String key)
	{
		return ConfigTypeUtils.getBoolean(findObject(key));
	}

	public Double findDouble(String key)
	{
		return ConfigTypeUtils.getDouble(findObject(key));
	}

	public Integer findInteger(String key)
	{
		return ConfigTypeUtils.getInteger(findObject(key));
	}

	@SuppressWarnings("unchecked")
	public List<Object> findList(String key)
	{
		/*
		 * May not help in making the nested Lists immutable, but that's OK for now.
		 */
		return Collections.unmodifiableList(ConfigTypeUtils.getList(findObject(key)));
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> findMap(String key)
	{
		/*
		 * May not help in making the nested Lists immutable, but that's OK for now.
		 */
		return Collections.unmodifiableMap(ConfigTypeUtils.getMap(findObject(key)));
	}

	public Object findObject(String key)
	{
		BranchNode node = root_.findNode(key, false); // don't create nodes if not found
		ValueNode value = null;
		if ((node != null) && ((value = node.getValue()) != null))
		{
			return value.value();
		}
		return null;
	}

	public Map<String, Object> findObjectsByPrefix(String keyPrefix)
	{
		Map<String, Object> result = new HashMap<String, Object>();
		List<ValueNode> valueNodes = findValueNodesByPrefix(keyPrefix);
		for (ValueNode node : valueNodes)
		{
			result.put(node.getKey().getKeyName(), node.value());
		}
		return result;
	}

	public String findString(String key)
	{
		return ConfigTypeUtils.getString(findObject(key));
	}

	/**
	 * The AppGroup name.
	 */
	public String getApplicationGroup()
	{
		return appGroup;
	}

	/**
	 * The Application name.
	 */
	public String getApplicationName()
	{
		return appName;
	}

	/**
	 * The application domain.
	 */
	public String getDomain()
	{
		return domain;
	}

	public void insertBoolean(String key, boolean value)
	{
		insertObject(key, Boolean.toString(value), PROGRAMMATIC_INSERTION);
	}

	public void insertDouble(String key, double value)
	{
		insertObject(key, Double.toString(value), PROGRAMMATIC_INSERTION);
	}

	public void insertInteger(String key, int value)
	{
		insertObject(key, Integer.toString(value), PROGRAMMATIC_INSERTION);
	}

	public void insertList(String key, List<Object> value)
	{
		insertObject(key, new Vector<Object>(value), PROGRAMMATIC_INSERTION);
	}

	public void insertMap(String key, Map<String, Object> value)
	{
		insertObject(key, new Hashtable<String, Object>(value), PROGRAMMATIC_INSERTION);
	}

	public void insertString(String key, String value)
	{
		insertObject(key, value, PROGRAMMATIC_INSERTION);
	}

	/**
	 * Determine if the application is in a production domain.
	 * 
	 * @return true if the domain is production-equivalent, false otherwise.
	 */
	public boolean isProduction()
	{
		return (domain.equals("prod"));
	}

	public void loadEntriesFromFile(String path, boolean required)
	{
		File file = new File(path);
		if (!file.exists())
		{
			if (required)
			{
				throw new ConfigSyntaxException("Required config file \"" + path + "\" not found");
			}
			// not found, not required....
			return;
		}

		try
		{
			logger.info("Parsing config file: " + path);
			List<ConfigEntry> kvPairs = ConfigParser.parseConfigFile(path);

			for (ConfigEntry pair : kvPairs)
			{
				String domainName = pair.key.getDomainName();
				if (!keepKeyForTree(domainName))
				{
					continue;
				}
				insertObject(pair.key, pair.value, path);
			}
		}
		catch (Exception e)
		{
			throw new ConfigSyntaxException("Failure encountered while parsing config file (" + path + ")", e);
		}
	}

	/**
	 * Generate a string containing all retrievable config entries. This string is in the syntax of
	 * a config file, and contains comments indicating where each value came from.
	 */
	public String dumpConfig()
	{
		StringBuffer out = new StringBuffer(1024);
		out.append("Domain = ").append(getDomain()).append("\n");
		out.append("Application Group = ").append(getApplicationGroup()).append("\n");
		out.append("Application Name = ").append(getApplicationName()).append("\n");
		out.append("Override Files = ").append(overrides_ != null ? overrides_.toString() : "[]").append("\n\n\n");

		root_.printTo(out);
		return out.toString();
	}

	/**
	 * Generate a string containing all retrievable config entries. This string is in the syntax of
	 * a config file, and contains comments indicating where each value came from.
	 */
	public String dumpConfig(String keyPrefix)
	{
		/*
		 * Calling this function with a null or empty string prefix is same as asking for a full
		 * dump. Redirect.
		 */
		if ((keyPrefix == null) || keyPrefix.trim().equals(""))
		{
			return dumpConfig();
		}

		StringBuffer out = new StringBuffer();
		List<ValueNode> valueNodes = findValueNodesByPrefix(keyPrefix);
		for (ValueNode valueNode : valueNodes)
		{
			valueNode.printTo(out);
		}
		return out.toString();
	}

	/**
	 * Find and return a BranchNode for the specified key. Create if it doesn't already exist.
	 */
	public BranchNode findNode(String key)
	{
		return root_.findNode(key, true);
	}

	private void assertOnConfigDirs()
	{
		String configDirs[] = new String[] { GLOBAL, APPGROUP, APP, OVERRIDE };
		for (String dirName : configDirs)
		{
			File configDir = new File(configRoot + File.separator + dirName);
			if (configDir.exists() == false)
			{
				throw new IllegalStateException("Config dir [" + configDir.getAbsolutePath() + "] does not exist");
			}

			if (configDir.isDirectory() == false)
			{
				throw new IllegalStateException("Config path [" + configDir.getAbsolutePath() + "] is not a directory");
			}

		}
	}

	private List<ValueNode> findValueNodesByPrefix(String keyPrefix)
	{
		if (keyPrefix.endsWith("."))
		{
			throw new ConfigSyntaxException("Invalid prefix: " + keyPrefix);
		}
		BranchNode prefixTreeRoot = root_.findNode(keyPrefix, false);

		return prefixTreeRoot == null ? new ArrayList<ValueNode>() : prefixTreeRoot.findAllValueNodes();
	}

	private void insertObject(ConfigKey key, Object value, String fileFrom)
	{
		if (value == null)
		{
			throw new ConfigSyntaxException("Can't insert null value (key= [" + key + "])");
		}

		BranchNode node = root_.findNode(key.getKeyName(), true);

		ValueNode valueNode = node.getValue();

		if (valueNode == null)
		{
			node.setValue(new ValueNode(key, value, fileFrom));
		}
		else
		{
			valueNode.insertAdditionalData(key, value, fileFrom);
		}
	}

	private void insertObject(String keyNameWithoutDomain, Object value, String fileFrom)
	{
		ConfigKey key = new ConfigKey(domain, keyNameWithoutDomain);
		insertObject(key, value, fileFrom);
	}

	/**
	 * Determine if this key should generally readable by this ConfigTree's domain. It may be
	 * overridden by another file, or another entry with more specific domain in the same file, but
	 * this function tells us if we should keep that value assuming we have no value for that
	 * partial key yet.
	 * 
	 * @param domainName
	 *            the domain of inbound config key.
	 * @return true if the domain/realm as consistent with this tree, false otherwise.
	 */
	private boolean keepKeyForTree(String domainName)
	{
		// if key's domain is *
		// or matches our domain
		return (domainName.equals("*") || domainName.equals(this.domain));
	}

	/**
	 * Given that all required information has been initialized, read in all config files into a
	 * newly construct tree of BranchNodes and ValueNodes.
	 */
	private void readConfigFiles()
	{
		assertOnConfigDirs();

		root_ = new BranchNode();

		File globalDir = new File(configRoot + File.separator + GLOBAL + File.separator);
		File[] globalFiles = globalDir.listFiles(new ConfigFileFilter());
		for (int i = 0; i < globalFiles.length; ++i)
		{
			String filepath = globalFiles[i].getAbsolutePath();
			loadEntriesFromFile(filepath, true);
		}

		String appgroupFile = configRoot + File.separator + APPGROUP + File.separator + appGroup + CONFIG_SUFFIX;
		loadEntriesFromFile(appgroupFile, true);

		String appFile = configRoot + File.separator + APP + File.separator + appName + CONFIG_SUFFIX;
		loadEntriesFromFile(appFile, true);

		for (int i = 0; i < overrides_.size(); ++i)
		{
			String overrideFileName = overrides_.get(i);
			String overrideFilePath = configRoot + File.separator + OVERRIDE + File.separator + overrideFileName;
			loadEntriesFromFile(overrideFilePath, false);
		}
	}

	
	public List<String> getOverrideFileNames()
	{
		return new ArrayList<String>(overrides_);
	}

}
