package org.myrobotlab.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.document.transformer.ConnectorConfig;
import org.myrobotlab.framework.ServiceType;

public class DatabaseConnector extends AbstractConnector {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private String driver;
  private String connectionString;
  private String jdbcUser;
  private String jdbcPassword;
  private String preSql;
  private String Sql;
  private String postSql;
  private String idField;
  private Connection connection = null;

  public DatabaseConnector(String name) {
    super(name);
  }

  @Override
  public void setConfig(ConnectorConfig config) {
    // TODO Auto-generated method stub
    log.info("Set Config not yet implemented");
  }

  // @Override
  // public void initialize(ConnectorConfiguration config) {
  // driver = config.getProperty("jdbcDriver");
  // connectionString = config.getProperty("connectionString");
  // jdbcUser = config.getProperty("jdbcUser");
  // jdbcPassword = config.getProperty("jdbcPassword");
  // idField = config.getProperty("idField");
  // preSql = config.getProperty("preSql");
  // Sql = config.getProperty("Sql");
  // postSql = config.getProperty("postSql");
  // // Create the connection
  // createConnection();
  // }

  private void createConnection() {
    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      connection = DriverManager.getConnection(connectionString, jdbcUser, jdbcPassword);
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void startCrawling() {
    // Here is where we start up our connector
    setState(ConnectorState.RUNNING);

    // connect to the database.
    createConnection();

    // run the pre-sql
    runPreSql();

    Statement state = null;
    try {
      state = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    //
    ResultSet rs = null;
    if (state != null) {
      try {
        rs = state.executeQuery(Sql);
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    if (rs != null) {
      String[] columns = null;
      try {
        columns = getColumnNames(rs);
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      int idColumn = -1;
      for (int i = 0; i < columns.length; i++) {
        if (columns[i].equalsIgnoreCase(idField)) {
          idColumn = i+1;
          break;
        }
      }

      try {
        while (rs.next()) {
          // Need the ID column from the RS.
          String id = rs.getString(idColumn);
          Document doc = new Document(id);
          // Add each column / field name to the doc
          for (int i = 0; i < columns.length; i++) {
            doc.addToField(columns[i], rs.getString(i+1));
          }
          // Process this row!
          feed(doc);
        }
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    // the post sql.
    runPostSql();

  }

  /**
   * Return an array of column names.
   */
  private String[] getColumnNames(ResultSet rs) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    String[] names = new String[meta.getColumnCount()];
    for (int i = 0; i < names.length; i++) {
      names[i] = meta.getColumnName(i + 1);
    }
    return names;
  }

  private void runPreSql() {
    if (preSql != null){
      try {
        Statement state = connection.createStatement();
        state.executeUpdate(preSql);
        state.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void runPostSql() {
    if (postSql != null){
      try {
        Statement state = connection.createStatement();
        state.executeUpdate(postSql);
        state.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Override
  public void stopCrawling() {
    // TODO Auto-generated method stub
    setState(ConnectorState.STOPPED);
  }

  public String getDriver() {
    return driver;
  }

  public void setDriver(String driver) {
    this.driver = driver;
  }

  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

  public String getJdbcUser() {
    return jdbcUser;
  }

  public void setJdbcUser(String jdbcUser) {
    this.jdbcUser = jdbcUser;
  }

  public String getJdbcPassword() {
    return jdbcPassword;
  }

  public void setJdbcPassword(String jdbcPassword) {
    this.jdbcPassword = jdbcPassword;
  }

  public String getPreSql() {
    return preSql;
  }

  public void setPreSql(String preSql) {
    this.preSql = preSql;
  }

  public String getSql() {
    return Sql;
  }

  public void setSql(String sql) {
    Sql = sql;
  }

  public String getPostSql() {
    return postSql;
  }

  public void setPostSql(String postSql) {
    this.postSql = postSql;
  }

  public String getIdField() {
    return idField;
  }

  public void setIdField(String idField) {
    this.idField = idField;
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

    ServiceType meta = new ServiceType(DatabaseConnector.class.getCanonicalName());
    meta.addDescription("This service will run a select statement against a database and return the rows as documents to be published");
    meta.addCategory("ingest");
    return meta;
  }

}
