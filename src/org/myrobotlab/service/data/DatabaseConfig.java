/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service.data;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class DatabaseConfig {
	// @SuppressWarnings("serial")
	private final static Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

	public int ID;

	public String URL_; // The full URL for the JDBC connection
	public String driverURL; // JDBC connection prefix
	public String driverName; // Name of class for checking Class.forName
	public String hostname; // Host Name of the RDBM server
	public int portNumber; // Port the server is listening on
	public String databaseName_; // Default Database
	public String userName; // User Login ID
	public String Password_; // Password
	public String SelectMethod_; // SelectMethod Informs the driver to use
	// server a side-cursor,which permits more
	// than one active statement on a connection
	// | should be option
	public String RDBMSType; // RDBMS Option

	// option constants
	public final static String driverNameCOM_MICROSOFT_JDBC_SQLSERVER_SQLSERVERDRIVER = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
	public final static String driverNameCOM_MYSQL_JDBC_DRIVER = "com.mysql.jdbc.Driver";
	public final static String DRIVERURL_JDBC_MICROSOFT_SQLSERVER = "jdbc:microsoft:sqlserver://";
	public final static String DRIVERURL_JDBC_MYSQL = "jdbc:mysql://";
	public final static String RDBMSTYPE_MSSQL = "MSSQL";
	public final static String RDBMSTYPE_MYSQL = "MYSQL";

	public static String name() {
		if (log.isDebugEnabled()) {
			StringBuilder logString = new StringBuilder("DatabaseConfig.getName()()");
			log.debug("{}", logString);
		} // if

		String ret = new String("DatabaseConfig");
		return ret;
	}

	public static String scope() {
		String ret = new String("dbms");
		return ret;
	}

	// ctors begin ----
	public DatabaseConfig() {

		URL_ = new String();
		driverURL = new String("jdbc:mysql://");
		driverName = new String("com.mysql.jdbc.Driver");
		hostname = new String();
		portNumber = 3306;
		databaseName_ = new String();
		userName = new String();
		Password_ = new String();
		SelectMethod_ = new String("cursor");
		RDBMSType = new String("MYSQL");

		/*
		 * driverName.setValidOption("com.microsoft.jdbc.sqlserver.SQLServerDriver"
		 * ,"com.microsoft.jdbc.sqlserver.SQLServerDriver");
		 * driverName.setValidOption
		 * ("com.mysql.jdbc.Driver","com.mysql.jdbc.Driver");
		 * driverURL.setValidOption
		 * ("jdbc:microsoft:sqlserver://","jdbc:microsoft:sqlserver://");
		 * driverURL.setValidOption("jdbc:mysql://","jdbc:mysql://");
		 * RDBMSType.setValidOption("MSSQL","MSSQL");
		 * RDBMSType.setValidOption("MYSQL","MYSQL");
		 */

	}

	// assignment end ---

	public DatabaseConfig(final DatabaseConfig other) {
		this();
		set(other);
	};

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final DatabaseConfig other) {
		ID = other.ID;
		URL_ = other.URL_;
		driverURL = other.driverURL;
		driverName = other.driverName;
		hostname = other.hostname;
		portNumber = other.portNumber;
		databaseName_ = other.databaseName_;
		userName = other.userName;
		Password_ = other.Password_;
		SelectMethod_ = other.SelectMethod_;
		RDBMSType = other.RDBMSType;

	};

	// todo format string interface more than one compact, ini, xml
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("<DatabaseConfig>");
		ret.append("<ID>" + ID + "</ID>");
		ret.append("<URL>" + URL_ + "</URL>");
		ret.append("<DriverURL>" + driverURL + "</DriverURL>");
		ret.append("<DriverName>" + driverName + "</DriverName>");
		ret.append("<HostName>" + hostname + "</HostName>");
		ret.append("<PortNumber>" + portNumber + "</PortNumber>");
		ret.append("<databaseName>" + databaseName_ + "</databaseName>");
		ret.append("<UserName>" + userName + "</UserName>");
		ret.append("<Password>" + Password_ + "</Password>");
		ret.append("<SelectMethod>" + SelectMethod_ + "</SelectMethod>");
		ret.append("<RDBMSType>" + RDBMSType + "</RDBMSType>");

		ret.append("</DatabaseConfig>");
		return ret.toString();
	}

}