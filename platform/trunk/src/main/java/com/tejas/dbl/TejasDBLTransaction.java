package com.tejas.dbl;

import com.tejas.core.TejasDBLayer;

/**
 * Yet another attempt to reduce the amount of code one has to type!
 */
public abstract class TejasDBLTransaction
{
    private TejasDBLayer _dbl;

    public TejasDBLTransaction(TejasDBLayer dbl)
    {
        this._dbl = dbl;
    }
    
    
    public void execute()
    {
        try
        {
            _dbl.startTransaction();
            doInTransaction(_dbl);
            _dbl.commit();
        }
        finally
        {
            _dbl.rollback();
        }
    }

    public abstract void doInTransaction(TejasDBLayer dbl);
}
