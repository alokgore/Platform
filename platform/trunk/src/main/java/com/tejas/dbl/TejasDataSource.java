package com.tejas.dbl;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;


public class TejasDataSource implements DataSource
{

	private final ComboPooledDataSource dataSource;
	
	public TejasDataSource(ComboPooledDataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return dataSource.getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException
	{
		return dataSource.getConnection(username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException
	{
		return dataSource.getLogWriter();
	}

	@Override
	public int getLoginTimeout() throws SQLException
	{
		return dataSource.getLoginTimeout();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException
	{
		dataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException
	{
		dataSource.setLoginTimeout(seconds);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return null;
	}
	
	public void close()
	{
		dataSource.close();
	}
}
