package com.tejas.core;

import org.apache.ibatis.session.TransactionIsolationLevel;

public interface TejasDBLayer extends Cloneable
{
    <T> T getMybatisMapper(Class<T> type);

    public void commit();

    public boolean isTransactional();

    public void rollback();

    public void startTransaction();

    public void startTransaction(TransactionIsolationLevel isolation);
}
