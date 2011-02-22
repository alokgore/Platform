package com.tejas.dbl;

import java.io.File;
import java.sql.DriverManager;

import org.sqlite.JDBC;

import com.tejas.utils.misc.Assert;

public class SqliteEndpoint extends DatabaseEndpoint
{
    @SuppressWarnings("rawtypes")
    public static class Builder
    {
        public Enum _endpointName;
        public String _databaseName = "tejas";
        public DatabaseEndpoint.EndpointType _endpointType = DatabaseEndpoint.EndpointType.READ_WRITE;
        public String _databaseLocation = "/var/tmp/sqlite/";

        public Builder withDatabaseLocation(String location)
        {
            _databaseLocation = location;
            return this;
        }

        public Builder(Enum name)
        {
            _endpointName = name;
        }

        public Builder withDatabaseName(String databaseName)
        {
            _databaseName = databaseName;
            return this;
        }

        public Builder withEndpointType(DatabaseEndpoint.EndpointType endpointType)
        {
            _endpointType = endpointType;
            return this;
        }

        public SqliteEndpoint build() throws Exception
        {
            String databaseDirPath = _databaseLocation;

            File dbDir = new File(databaseDirPath);
            dbDir.mkdirs();
            Assert.isTrue(dbDir.exists() && dbDir.isDirectory());

            String jdbcURL = "jdbc:sqlite:" + dbDir + "/" + _databaseName + ".db";
            return new SqliteEndpoint(_endpointName, jdbcURL, _endpointType);
        }
    }

    @SuppressWarnings("rawtypes")
    SqliteEndpoint(Enum name, String jdbcURL, DatabaseEndpoint.EndpointType endPointType) throws Exception
    {
        super(name, endPointType, DatabaseVendor.SQLite, new EndPointDefinition(jdbcURL, new JDBC()));
        DriverManager.registerDriver(new JDBC());
    }
}
