package org.myrobotlab.service;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * @author GRPERRY
 * 
 *         A Service for sending email
 * 
 *         References :
 *         http://www.mkyong.com/java/javamail-api-sending-email-via
 *         -gmail-smtp-example/
 * 
 */
public class Mail extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Mail.class.getCanonicalName());

  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);

    try {
      Mail mail = new Mail("mail");
      mail.startService();
      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public static void sendMailSSL() {
    Properties props = new Properties();
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.socketFactory.port", "465");
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.port", "465");

    Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("username", "password");
      }
    });

    try {

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress("from@no-spam.com"));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("to@no-spam.com"));
      message.setSubject("Testing Subject");
      message.setText("Dear Mail Crawler," + "\n\n No spam to my email, please!");

      Transport.send(message);

      System.out.println("Done");

    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public static void sendMailTLS() {
    final String username = "username@gmail.com";
    final String password = "password";

    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587");

    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });

    try {

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress("from-email@gmail.com"));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("to-email@gmail.com"));
      message.setSubject("Testing Subject");
      message.setText("Dear Mail Crawler," + "\n\n No spam to my email, please!");

      Transport.send(message);

      System.out.println("Done");

    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public Mail(String n) {
    super(n);
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

    ServiceType meta = new ServiceType(Mail.class.getCanonicalName());
    meta.addDescription("General service for all your mail needs");
    meta.addCategory("connectivity");

    return meta;
  }

}
