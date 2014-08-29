package org.myrobotlab.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class MySQL extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MySQL.class);

	private Connection conn = null;

	public MySQL(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general mysql";
	}

	public boolean connect(String connStr) throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(connStr);
		if (conn != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean connect(String connStr, String user, String password) throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(connStr, user, password);
		if (conn != null) {
			return true;
		} else {
			return false;
		}
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		Statement statement = conn.createStatement();
		ResultSet resultSet = statement.executeQuery(sql);
		return resultSet;
	}
	
	public void test() throws ClassNotFoundException, SQLException {
		MySQL mysql = (MySQL) Runtime.start(getName(), "MySQL");
		mysql.connect("jdbc:mysql://localhost/mydatabase?" + "user=root&password=");
		
		String ip = "166.137.209.167";
		String sql = String.format("SELECT users.name, sessions.uid, sessions.hostname FROM myrobotlab.users INNER JOIN myrobotlab.sessions ON sessions.uid=users.uid WHERE sessions.hostname = '%s'", ip);
		ResultSet records = mysql.executeQuery(sql);
		String user = null;
		while (records.next()) {
			user = records.getString("name");
			log.info(user);
		}
		if (user == null){
			user = ip;
		}
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			MySQL mysql = (MySQL) Runtime.start("mysql", "MySQL");
			mysql.test();

			// Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
