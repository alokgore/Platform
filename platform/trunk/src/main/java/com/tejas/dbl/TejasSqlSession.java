package com.tejas.dbl;

import java.sql.Connection;
import java.util.List;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import com.tejas.types.exceptions.AccessControlException;


public class TejasSqlSession implements SqlSession
{
    private final boolean isReadOnly;
    private final SqlSession session;

    public TejasSqlSession(SqlSession session, boolean isReadOnly)
    {
        this.session = session;
        this.isReadOnly = isReadOnly;

    }

    private void checkWriteAccess()
    {
        if (isReadOnly)
        {
            throw new AccessControlException("This DBLayer Instance is Read-Only");
        }
    }

    @Override
    public void clearCache()
    {
        session.clearCache();
    }

    @Override
    public void close()
    {
        session.close();
    }

    @Override
    public void commit()
    {
        checkWriteAccess();
        session.commit();
    }

    @Override
    public void commit(boolean force)
    {
        checkWriteAccess();
        session.commit(force);
    }

    @Override
    public int delete(String statement)
    {
        checkWriteAccess();
        return session.delete(statement);
    }

    @Override
    public int delete(String statement, Object parameter)
    {
        checkWriteAccess();
        return session.delete(statement, parameter);
    }

    @Override
    public Configuration getConfiguration()
    {
        return session.getConfiguration();
    }

    @Override
    public Connection getConnection()
    {
        return session.getConnection();
    }

    @Override
    public <T> T getMapper(Class<T> type)
    {
        return session.getMapper(type);
    }

    @Override
    public int insert(String statement)
    {
        checkWriteAccess();
        return session.insert(statement);
    }

    @Override
    public int insert(String statement, Object parameter)
    {
        checkWriteAccess();
        return session.insert(statement, parameter);
    }

    @Override
    public void rollback()
    {
        session.rollback();
    }

    @Override
    public void rollback(boolean force)
    {
        session.rollback(force);
    }

    @Override
    public void select(String statement, Object parameter, ResultHandler handler)
    {
        session.select(statement, parameter, handler);
    }

    @Override
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler)
    {
        session.select(statement, parameter, rowBounds, handler);
    }

    @Override
    public void select(String statement, ResultHandler handler)
    {
        session.select(statement, handler);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List selectList(String statement)
    {
        return session.selectList(statement);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List selectList(String statement, Object parameter)
    {
        return session.selectList(statement, parameter);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List selectList(String statement, Object parameter, RowBounds rowBounds)
    {
        return session.selectList(statement, parameter, rowBounds);
    }

    @Override
    public Object selectOne(String statement)
    {
        return session.selectOne(statement);
    }

    @Override
    public Object selectOne(String statement, Object parameter)
    {
        return session.selectOne(statement, parameter);
    }

    @Override
    public int update(String statement)
    {
        checkWriteAccess();
        return session.update(statement);
    }

    @Override
    public int update(String statement, Object parameter)
    {
        checkWriteAccess();
        return session.update(statement, parameter);
    }
}
