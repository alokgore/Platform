package com.tejas.dbl;

import static com.tejas.dbl.DatabaseEndpoint.EndpointType.READ_WRITE;

import com.mysql.jdbc.Driver;

public class MySQLEndpoint extends DatabaseEndpoint
{
    public static class Builder
    {
        @SuppressWarnings("rawtypes")
        private final Enum _endpointName;
        private EndpointType _type = READ_WRITE;

        private String _hostName = "localhost";
        private int _port = 3306;
        private String _databaseName = "";
        private String _userName = "root";
        private String _password = "";

        @SuppressWarnings("rawtypes")
        public Builder(Enum endpointName)
        {
            this._endpointName = endpointName;
        }

        public MySQLEndpoint build() throws Exception
        {
            String jdbcURL = "jdbc:mysql://" + _hostName + ":" + _port + "/" + _databaseName;
            return new MySQLEndpoint(_endpointName, _type, _userName, _password, jdbcURL);
        }

        public Builder withDatabaseName(String databaseName)
        {
            this._databaseName = databaseName;
            return this;
        }

        public Builder withHostName(String hostName)
        {
            this._hostName = hostName;
            return this;
        }

        public Builder withPassword(String password)
        {
            this._password = password;
            return this;
        }

        public Builder withPort(int port)
        {
            this._port = port;
            return this;
        }

        public Builder withType(EndpointType type)
        {
            this._type = type;
            return this;
        }

        public Builder withUserName(String userName)
        {
            this._userName = userName;
            return this;
        }
    }

    @SuppressWarnings("rawtypes")
    MySQLEndpoint(Enum name, EndpointType type, String username, String password, String jdbcURL) throws Exception
    {
        super(name, type, DatabaseVendor.MySQL, new EndPointDefinition(jdbcURL, new Driver(), username, password));
    }
}
