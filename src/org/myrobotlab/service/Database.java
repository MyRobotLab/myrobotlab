package org.myrobotlab.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Database extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Database.class);

  public static String driver = "com.mysql.jdbc.Driver";
  public static String connectionString = ""; // e.g.// "jdbc:mysql://HOST/DATABASE"
  public static String jdbcUser = "user";
  public static String jdbcPassword = "password";
  static Connection connection = null;

  public Database(String n) {
    super(n);
  }

  public static void connect() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

    Class.forName(driver);
    connection = DriverManager.getConnection(connectionString, jdbcUser, jdbcPassword);

  }

  public void closeConnection() throws SQLException {
    connection.close();
  }

  public int executeUpdate(String sql) throws SQLException {
    Statement statement = connection.createStatement();
    // execute insert SQL statement
    return statement.executeUpdate(sql);
  }

  public static ResultSet executeQuery(String sql) throws SQLException {
    Statement statement = connection.createStatement();
    // execute insert SQL statement
    return statement.executeQuery(sql);
  }

  public boolean execute(String sql) throws SQLException {
    Statement statement = connection.createStatement();
    // execute insert SQL statement
    return statement.execute(sql);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Database.class.getCanonicalName());
    meta.addDescription("database - wrapper around jdbc access");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("storage");
    return meta;
  }

  public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			//connect();
			//ResultSet rs = executeQuery("SELECT * FROM uptime");
			//while (rs.next()) {
			//log.info(rs.getString("id"));			  
			//}
  }
}
