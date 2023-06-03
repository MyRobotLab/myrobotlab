package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.EmailConfig;
import org.myrobotlab.service.data.ImageData;
import org.slf4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 
 * Basic smtp at the moment. It can send a email with image through gmail.
 * 
 * A different client that has more access might be this ...
 * https://github.com/google/gdata-java-client
 * 
 * @author grog
 *
 */
public class Email extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Email.class);

  public Email(String n, String id) {
    super(n, id);
  }

  public Map<String, String> setGmailProps(String user, String password) {
    Map<String, String> props = ((EmailConfig) this.config).properties;
    props.put("mail.smtp.user", user);
    props.put("mail.smtp.pass", password);
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587"); // 465
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.debug", "true");
    props.put("mail.smtp.starttls.required", "true");
    props.put("mail.smtp.ssl.protocols", "TLSv1.2");
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

    return props;
  }
  

  /**
   * Sends an email of an image. To, From ect are required to 
   * be setup in config, designed to be the recipient of subscribed
   * image publisher. Must be non encoded filesystem image file.
   * 
   * TODO - implemented encoded images to be sent, base64 or url references.
   * 
   * @param img
   */
  public void onImage(ImageData img) {
    sendHtmlMail(null, null, img.src, null, img.src);
  }

  public void sendImage(String to, String imageFile) {
    sendHtmlMail(null, to, imageFile, null, imageFile);
  }

  /**
   * 
   * @param to
   * @param subject
   * @param body
   * @param imageFile
   */

  public void sendMail(String to, String subject, String body, String imageFile) {
    EmailConfig config = (EmailConfig) this.config;

    Properties props = new Properties();
    props.putAll(config.properties);
    sendTextMail(config.properties.get("mail.smtp.user"), to, subject, body, config.format, null);
  }

  public void sendHtmlMail(String from, String to, String subject, String body, String imageFileName) {
    try {

      if (body == null) {
        body = "";
      }

      EmailConfig config = (EmailConfig) this.config;
      
      if (to == null) {
        to = config.to;
      }

      Properties props = new Properties();
      props.putAll(config.properties);

      Session session = Session.getDefaultInstance(props);

      // Create a default MimeMessage object.
      Message msg = new MimeMessage(session);

      // TODO attachements getTransport(smtps ?) - non auth ?
      // This mail has 2 part, the BODY and the embedded image
      MimeMultipart multipart = new MimeMultipart("related");

      // first part (the html)
      BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setContent(body, "text/html");
      // add it
      multipart.addBodyPart(messageBodyPart);

      // second part (the image)
      messageBodyPart = new MimeBodyPart();
      DataSource fds = new FileDataSource(imageFileName);

      messageBodyPart.setDataHandler(new DataHandler(fds));
      messageBodyPart.setHeader("Content-ID", "<image>");

      // add image to the multipart
      multipart.addBodyPart(messageBodyPart);

      // put everything together
      msg.setContent(multipart);

      ///////////////////////////////

      // creates a new e-mail message
      // Message msg = new MimeMessage(session);

      if (from != null) {
        msg.setFrom(new InternetAddress(from));
      }
      InternetAddress[] toAddresses = { new InternetAddress(to) };
      msg.setRecipients(Message.RecipientType.TO, toAddresses);
      msg.setSubject(subject);
      msg.setSentDate(new Date());
      // ONLY DIFF ???
      // msg.setText(body);

      Transport t = session.getTransport("smtp");
      // t.connect(host, userName, password);
      // t.connect();
      // t.connect(host, userName, password);
      // THIS IS RIDICULOUS - THEY SUPPLY A BAJILLION SESSION PROPS INCLUDING
      // USERNAME & PASSWORD
      // BUT THEY HAVE TO BE PULLED BACK OUT IN ORDER TO DO THE TRANSPORT ???

      String user = props.getProperty("mail.smtp.user");
      String password = props.getProperty("mail.smtp.pass");
      String host = props.getProperty("mail.smtp.host");
      String port = props.getProperty("mail.smtp.port");
      if (user == null) {
        error("user must be set");
      }
      if (password == null) {
        error("password must be set");
      }
      if (user == null) {
        error("host must be set");
      }
      if (user == null) {
        error("port must be set");
      }

      t.connect(host, Integer.parseInt(port), user, password);

      t.sendMessage(msg, msg.getAllRecipients());
      t.close();
    } catch (Exception e) {
      error(e);
    }

  }

  public void sendTextMail(String from, String to, String subject, String body, String format, List<Object> attachments) {
    try {
      EmailConfig config = (EmailConfig) this.config;

      Properties props = new Properties();
      props.putAll(config.properties);

      Session session = Session.getDefaultInstance(props);

      // TODO read and write out the session (including defaults)
      session.setDebug(true);

      // TODO attachements getTransport(smtps ?) - non auth ?

      // creates a new e-mail message
      Message msg = new MimeMessage(session);

      msg.setFrom(new InternetAddress(from));
      InternetAddress[] toAddresses = { new InternetAddress(to) };
      msg.setRecipients(Message.RecipientType.TO, toAddresses);
      msg.setSubject(subject);
      msg.setSentDate(new Date());
      msg.setText(body);

      Transport t = session.getTransport("smtp");
      // t.connect(host, userName, password);
      // t.connect();
      // t.connect(host, userName, password);
      // THIS IS RIDICULOUS - THEY SUPPLY A BAJILLION SESSION PROPS INCLUDING
      // USERNAME & PASSWORD
      // BUT THEY HAVE TO BE PULLED BACK OUT IN ORDER TO DO THE TRANSPORT ???

      String user = props.getProperty("mail.smtp.user");
      String password = props.getProperty("mail.smtp.pass");
      String host = props.getProperty("mail.smtp.host");
      String port = props.getProperty("mail.smtp.port");
      if (user == null) {
        error("user must be set");
      }
      if (password == null) {
        error("password must be set");
      }
      if (user == null) {
        error("host must be set");
      }
      if (user == null) {
        error("port must be set");
      }

      t.connect(host, Integer.parseInt(port), user, password);

      t.sendMessage(msg, msg.getAllRecipients());
      t.close();
    } catch (Exception e) {
      error(e);
    }

  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Email email = (Email) Runtime.start("email", "Email");

      email.setGmailProps("myuser@gmail.com", "xxxxxxxxx");
      email.sendImage("some-email@email.com", "data/OpenCV/cv-00573.png");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
