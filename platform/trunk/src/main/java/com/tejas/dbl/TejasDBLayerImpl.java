package com.tejas.dbl;

import static com.tejas.dbl.DatabaseEndpoint.EndpointType.READ_ONLY;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.TransactionIsolationLevel;

import com.tejas.core.TejasDBLayer;
import com.tejas.types.exceptions.AccessControlException;
import com.tejas.utils.misc.Assert;


public class TejasDBLayerImpl implements TejasDBLayer
{
    class SQLSessionHolder
    {
        private SqlSession session;
        private final boolean isReadOnly;

        public SQLSessionHolder(boolean isReadOnly)
        {
            this.isReadOnly = isReadOnly;
        }

        /**
         * If the DBLayer is not transactional, this method closes the SqlSession. For a transactional DBLayer, this method is a NO-OP (Does not touch the
         * session)
         */
        void checkinSession(SqlSession localSession)
        {
            if (localSession != this.session)
            {
                try
                {
                    localSession.close();
                }
                catch (Exception e)
                {
                    // Ignore
                }
            }
        }

        /**
         * Closes and nullifies the session (Ignoring any exception that might get thrown)
         */
        void closeSession()
        {
            try
            {
                if (session != null)
                {
                    session.close();
                }
            }
            catch (Exception e)
            {
                // Ignore
            }
            session = null;
        }

        final boolean isReadOnly()
        {
            return isReadOnly;
        }

        /**
         * @return SqlSession assosiated with the transaction Or a new SqlSession if the DBLayer is not transactional
         */
        public SqlSession checkoutSession()
        {
            if (session != null)
            {
                return session;
            }
            return new TejasSqlSession(endpoint.sqlSessionFactory.openSession(true), isReadOnly);
        }

        public void checkWriteAccess()
        {
            Assert.isFalse(isReadOnly, new AccessControlException("This DBLayer instance is read-only"));
        }

        public void commit()
        {
            Assert.notNull(session, "There is no active transaction to commit");
            session.commit();
            closeSession();
        }

        public boolean isTransactional()
        {
            return session != null;
        }

        public void rollback()
        {
            try
            {
                if (session != null)
                {
                    session.rollback();
                }
            }
            catch (Exception e)
            {
                // Ignore
            }
            closeSession();
        }

        public void startTransaction()
        {
            startTransaction(TransactionIsolationLevel.READ_COMMITTED);
        }

        public void startTransaction(TransactionIsolationLevel isolationLevel)
        {
            checkWriteAccess();
            Assert.isNull(session, "This DBLayer is already in a transaction (And we do not support nesting yet)");
            session = new TejasSqlSession(endpoint.sqlSessionFactory.openSession(ExecutorType.SIMPLE, isolationLevel), false);
        }
    }

    final DatabaseEndpoint endpoint;
    private final SQLSessionHolder sessionHolder;

    TejasDBLayerImpl(DatabaseEndpoint endpoint)
    {
        this(endpoint, false);
    }

    TejasDBLayerImpl(DatabaseEndpoint endpoint, boolean readOnly)
    {
        this.endpoint = endpoint;
        boolean isReadOnly = readOnly || (endpoint.type == READ_ONLY);
        this.sessionHolder = new SQLSessionHolder(isReadOnly);
    }

    final DatabaseEndpoint getEndpoint()
    {
        return endpoint;
    }

    final boolean isReadOnly()
    {
        return sessionHolder.isReadOnly();
    }

    @Override
    public TejasDBLayer clone()
    {
        return new TejasDBLayerImpl(this.endpoint, this.isReadOnly());
    }

    @Override
    public void commit()
    {
        sessionHolder.commit();
    }

    @Override
    public <T> T getMybatisMapper(Class<T> type)
    {
        endpoint.addMapper(type);
        return TejasMapperProxy.newMapperProxy(type, sessionHolder);
    }

    @Override
    public boolean isTransactional()
    {
        return sessionHolder.isTransactional();
    }

    @Override
    public void rollback()
    {
        sessionHolder.rollback();
    }

    @Override
    public void startTransaction()
    {
        sessionHolder.startTransaction();
    }

    @Override
    public void startTransaction(TransactionIsolationLevel isolation)
    {
        sessionHolder.startTransaction(isolation);
    }

}
