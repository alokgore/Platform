package com.tejas.dbl.types;

import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class BigIntegerTypeHandler extends BaseTypeHandler
{

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
    {
        return new BigInteger(cs.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException
    {
        return new BigInteger(rs.getString(columnName));
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException
    {
        ps.setObject(i, parameter, Types.BIGINT);
    }

}
