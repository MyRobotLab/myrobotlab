package org.myrobotlab.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Shoutbox.NameProvider;
import org.slf4j.Logger;

public class DrupalNameProvider implements NameProvider {
  public static final Logger log = LoggerFactory.getLogger(DrupalNameProvider.class);
  private Connection conn = null;

  @Override
  public String getName(String ip) {
    log.info(String.format("==DrupalNameProvider.getName(%s)==", ip));
    try {
      if (this.conn == null) {
        Class.forName("com.mysql.jdbc.Driver");
        log.info("attempting to connect to mysql");
        this.conn = DriverManager.getConnection("jdbc:mysql://localhost/myrobotlab", "root", "");
        if (this.conn == null) {
          log.error("could not connect");
          return ip;
        }
      }
      String sql = String.format("SELECT users.name, sessions.uid, sessions.hostname FROM myrobotlab.users " + " INNER JOIN myrobotlab.sessions ON sessions.uid=users.uid "
          + " WHERE sessions.hostname = '%s' " + " ORDER BY sessions.uid DESC", ip);

      Statement statement = this.conn.createStatement();
      log.info("executing query");
      ResultSet records = statement.executeQuery(sql);

      String user = null;
      while (records.next()) {
        user = records.getString("name");
        log.info(String.format("found [%s] for ip %s", user, ip));
        if ((user == null) || (user.trim().length() == 0 || user.trim().equals(""))) {
          log.info("user null or blank skipping");
          continue;
        } else {
          log.info(String.format("found user [%s]", user));
          return user;
        }
      }
      log.info(String.format("no not blank records found returning ip [%s]", ip));
      return ip;
    } catch (Exception e) {
      Logging.logError(e);
    }
    return ip;
  }
}
