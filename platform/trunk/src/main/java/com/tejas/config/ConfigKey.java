package com.tejas.config;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a key in the Tejas Configuration. Tracks the domain-name and multi-level key name
 * separately. e.g. A key of the form "prod.a.b.c" will become {domain=prod, key='a.b.c'}
 * 
 * @author alokgore
 */
class ConfigKey
{
	public static class Domain
	{
		/**
		 * Can be a specific domain-name (like 'prod' or 'sandbox') or '*'
		 */
		private final String domainName;

		public Domain(String domainName)
		{
			this.domainName = domainName;
		}

		/**
		 * Rules of the game <br>
		 * <ul>
		 * <li>Domain '*' can override domain '*'
		 * <li>Domain 'xyz' can override domain '*'
		 * <li>Domain 'xyz' can override domain 'xyz'
		 * <li>Domain '*' CAN NOT override domain 'xyz'
		 * <li>If domain 'xyz' is trying to override domain 'abc', someone up there has a bug in the
		 * code
		 * </ul>
		 */
		public boolean canOverride(Domain otherDomain)
		{
			if (this.equals(otherDomain) == false)
			{
				/*
				 * This function is called in a very specific context (namely, when someone is
				 * trying to override the '*.xyz=abc;' entry in the config with 'domain.xyz=ABC;'
				 * entry. The following is just an assertion of this assumption
				 */
				if (this.isStar() == false && otherDomain.isStar() == false)
				{
					throw new IllegalStateException("Comparting two specific domains [" + this + "] and [" + otherDomain
							+ "]!! Smells like a bug! Die!");
				}
			}
			return this.equals(otherDomain) || otherDomain.isStar();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Domain)
			{
				Domain otherDomain = (Domain) obj;
				return this.domainName.equals(otherDomain.domainName);
			}
			return false;
		}

		public String getDomainName()
		{
			return domainName;
		}

		@Override
		public int hashCode()
		{
			return this.domainName.hashCode();
		}

		@Override
		public String toString()
		{
			return domainName;
		}

		private boolean isStar()
		{
			return domainName.equals("*");
		}
	}

	/**
	 * Indicates that the config file contains a "+" sign at the end of the key.<br>
	 * This means that the value associated with this this key (Map or List) is to be merged with
	 * the old value associated with this key
	 */
	private boolean appendFlag = false;

	private Domain domain;

	private String hierarchicalKeyName;

	/**
	 * @param fullyQualifiedKeyName
	 *            Key name along with the domain-name (As keys appear in the config flles) <br>
	 *            Can contain a '+' sign in the end to indicate that the config values are to be
	 *            appended to the old values <br>
	 *            Example :
	 *            <ul>
	 *            <li>'prod.cms.database.primary'</li>
	 *            <li>'*.cms.book.templateName'</li>
	 *            <li>'*.cms.book.table.fields+'</li>
	 *            </ul>
	 */
	public ConfigKey(String fullyQualifiedKeyName)
	{
		int indexOfDot = fullyQualifiedKeyName.indexOf('.');
		String domainName = fullyQualifiedKeyName.substring(0, indexOfDot);
		String keyName = fullyQualifiedKeyName.substring(indexOfDot + 1);
		init(domainName, keyName);
	}

	public ConfigKey(String domain, String key)
	{
		init(domain, key);
	}

	public Domain getDomain()
	{
		return domain;
	}

	/**
	 * @return Domain name (or '*')
	 */
	public String getDomainName()
	{
		return domain.getDomainName();
	}

	/**
	 * @return Hierarchical key name WITH domain (e.g. 'prod.cms.categories.book')
	 */
	public String getFullyQualifiedKeyName()
	{
		return getDomainName() + "." + getKeyName();
	}

	/**
	 * @return A List of key components<br>
	 *         E.g. Key "x.y.z" will have 3 components "x" , "y" and "z"
	 */
	public List<String> getKeyComponents()
	{
		return Arrays.asList(hierarchicalKeyName.split("."));
	}

	/**
	 * @return Hierarchical key name without domain (e.g. 'cms.categories.book')
	 */
	public String getKeyName()
	{
		return hierarchicalKeyName;
	}

	/**
	 * Indicates that the config file contains a "+" sign at the end of the key.<br>
	 * This means that the value associated with this this key (Map or List) is to be merged with
	 * the old value associated with this key
	 */
	public boolean isAppendFlagSet()
	{
		return appendFlag;
	}

	@Override
	public String toString()
	{
		return getFullyQualifiedKeyName();
	}

	private void init(String domainName, String key)
	{
		this.domain = new Domain(domainName);
		this.hierarchicalKeyName = key;

		if (hierarchicalKeyName.endsWith("+"))
		{
			appendFlag = true;
			this.hierarchicalKeyName = key.substring(0, key.length() - 1);
		}

		validateKeyName(this.hierarchicalKeyName);
	}

	private void validateKeyName(String keyName)
	{
		if (keyName.startsWith(".") || keyName.endsWith(".") || keyName.indexOf('*') != -1)
		{
			throw new IllegalArgumentException("Illegal key-name [" + keyName + "]");
		}
	}
}
