package com.tejas.dbl;

import static com.tejas.dbl.DatabaseEndpoint.EndpointType.READ_WRITE;

import com.mysql.jdbc.Driver;

public class HSQLEndPoint extends DatabaseEndpoint {

	public static class Builder {

		@SuppressWarnings("rawtypes")
		private final Enum _endpointName;
		private String _userName = "root";
		private String _password = "";

		private String _databaseName = "";
		private EndpointType _type = READ_WRITE;
		public String _databaseLocation = "/var/DBtemp/";

		@SuppressWarnings("rawtypes")
		public Builder(Enum endpointName) {
			this._endpointName = endpointName;
		}

		public Builder withPassword(String password) {
			this._password = password;
			return this;
		}

		public Builder withDatabaseLocation(String databaseLocation) {
			this._databaseLocation = databaseLocation;
			return this;
		}

		public Builder withUserName(String userName) {
			this._userName = userName;
			return this;
		}

		public Builder withDatabaseName(String databaseName) {
			this._databaseName = databaseName;
			return this;
		}

		public Builder withType(EndpointType type) {
			this._type = type;
			return this;
		}

		public HSQLEndPoint build() throws Exception {
			String jdbcURL = "jdbc:hsqldb:file:" + this._databaseLocation
					+ this._databaseName;
			return new HSQLEndPoint(_endpointName, _type, _userName, _password,
					jdbcURL);
		}
	}

	@SuppressWarnings("rawtypes")
	HSQLEndPoint(Enum name, EndpointType type, String username,
			String password, String jdbcURL) throws Exception {
		super(name, type, DatabaseVendor.HSQLDB, new EndPointDefinition(
				jdbcURL, new Driver(), username, password));
	}

}
