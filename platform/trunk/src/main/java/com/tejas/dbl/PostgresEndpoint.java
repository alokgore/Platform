package com.tejas.dbl;

import org.postgresql.Driver;

public class PostgresEndpoint extends DatabaseEndpoint
{

    public static class Builder
    {
        @SuppressWarnings("rawtypes")
        private final Enum _endpointName;
        private EndpointType _type = EndpointType.READ_WRITE;

        private String _hostName = "localhost";
        private int _port = 5488;
        private String _databaseName = "postgres";
        private String _userName = "postgres";
        private String _password = "sql123";

        @SuppressWarnings("rawtypes")
        public Builder(Enum endpointName)
        {
            _endpointName = endpointName;
        }

        public PostgresEndpoint build() throws Exception
        {
            System.out.println("Trying to build");
            String jdbcURL = "jdbc:postgresql://" + _hostName + ":" + _port + "/" + _databaseName;
            return new PostgresEndpoint(_endpointName, _type, _userName, _password, jdbcURL);
        }

        public Builder withDatabaseName(String databaseName)
        {
            _databaseName = databaseName;
            return this;
        }

        public Builder withHostName(String hostName)
        {
            _hostName = hostName;
            return this;
        }

        public Builder withPassword(String password)
        {
            _password = password;
            return this;
        }

        public Builder withPort(int port)
        {
            _port = port;
            return this;
        }

        public Builder withType(EndpointType type)
        {
            _type = type;
            return this;
        }

        public Builder withUserName(String userName)
        {
            _userName = userName;
            return this;
        }
    }

    @SuppressWarnings("rawtypes")
    PostgresEndpoint(Enum name, EndpointType type, String username, String password, String jdbcURL) throws Exception
    {
        super(name, type, DatabaseVendor.POSTGRES, new EndPointDefinition(jdbcURL, new Driver(), username, password));
    }

}
