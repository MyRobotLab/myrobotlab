package org.myrobotlab.service;

import java.util.Date;
import java.util.Properties;

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

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.EmailConfig;
import org.myrobotlab.service.data.ImageData;
import org.slf4j.Logger;

/**
 * 
 * Basic smtp at the moment. It can send a email with image through gmail.
 * 
 * A different client that has more access might be this ...
 * https://github.com/google/gdata-java-client
 * 
 * @author GroG
 *
 */
public class Email extends Service<EmailConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Email.class);

  public Email(String n, String id) {
    super(n, id);
  }

  Properties props = new Properties();

  public Object addProperty(String name, String value) {
    return props.setProperty(name, value);
  }

  public Object removeProperty(String name) {
    return props.remove(name);
  }

  public Properties setGmailProps(String user, String password) {

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
   * Sends an email of an image. To, From ect are required to be setup in
   * config, designed to be the recipient of subscribed image publisher. Must be
   * non encoded filesystem image file.
   * 
   * TODO - implemented encoded images to be sent, base64 or url references.
   * 
   * @param img
   */
  public void onImage(ImageData img) {
    sendHtmlMail(null, null, img.src, null, img.src);
  }
  
  public void sendImage(String imageFile) {
    sendHtmlMail(null, null, imageFile, null, imageFile);
  }

  public void sendImage(String to, String imageFile) {
    sendHtmlMail(null, to, imageFile, null, imageFile);
  }

  public void sendEmail(String to, String subject, String body) {
    sendEmail(to, subject, body, "html", null);
  }

  public void sendEmail(String to, String subject, String body, String imageFile) {
    sendEmail(to, subject, body, "html", imageFile);
  }

  /**
   * 
   * @param to
   * @param subject
   * @param body
   * @param imageFile
   */

  public void sendEmail(String to, String subject, String body, String format, String imageFile) {
    if ("text".equals(format)) {
      sendTextMail((String) props.get("mail.smtp.user"), to, subject, body, imageFile);
    } else {
      sendHtmlMail((String) props.get("mail.smtp.user"), to, subject, body, imageFile);
    }
  }

  public void sendHtmlMail(String from, String to, String subject, String body, String imageFileName) {
    try {

      if (body == null) {
        body = "";
      }

      if (to == null) {
        to = config.to;
      }

      if (from == null) {
        from = config.from;
      }

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
      if (imageFileName != null && !imageFileName.isEmpty()) {
        messageBodyPart = new MimeBodyPart();
        DataSource fds = new FileDataSource(imageFileName);

        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID", "<image>");

        // add image to the multipart
        multipart.addBodyPart(messageBodyPart);
      }

      // put everything together
      msg.setContent(multipart);

      ///////////////////////////////

      // creates a new e-mail message
      // Message msg = new MimeMessage(session);

      if (from != null && !from.isEmpty()) {
        msg.setFrom(new InternetAddress(from));
      }
      InternetAddress[] toAddresses = { new InternetAddress(to) };
      msg.setRecipients(Message.RecipientType.TO, toAddresses);
      msg.setSubject(subject);
      msg.setSentDate(new Date());
      // ONLY DIFF ???
      // msg.setText(body);

      Transport t = session.getTransport("smtp");

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

      log.info("connecting to host");
      t.connect(host, Integer.parseInt(port), user, password);
      log.info("sending email message");

      t.sendMessage(msg, msg.getAllRecipients());
      log.info("closing session");
      t.close();
    } catch (Exception e) {
      error(e);
    }

  }

  public void sendTextMail(String from, String to, String subject, String body, String attachment) {
    try {

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

  /**
   * Load the config into memory
   */
  @Override
  public EmailConfig getConfig() {
    super.getConfig();

    config.auth = (String) props.getOrDefault("mail.smtp.auth", "true");
    config.debug = (String) props.getOrDefault("mail.smtp.debug", "true");
    config.host = (String) props.getOrDefault("mail.smtp.host", config.host);
    config.pass = (String) props.getOrDefault("mail.smtp.pass", config.pass);
    config.port = (String) props.getOrDefault("mail.smtp.port", config.port);
    config.protocols = (String) props.getOrDefault("mail.smtp.ssl.protocols", "TLSv1.2");
    config.socketFactory = (String) props.getOrDefault("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    config.starttlsEnabled = (String) props.getOrDefault("mail.smtp.starttls.enable", "true");
    config.starttlsRequired = (String) props.getOrDefault("mail.smtp.starttls.required", "true");
    config.user = (String) props.getOrDefault("mail.smtp.user", config.user);

    return config;
  }

  /**
   * Applies the config to the service by unloading the config object data into
   * props.
   */
  @Override
  public EmailConfig apply(EmailConfig c) {
    super.apply(c);

    props.put("mail.smtp.auth", c.auth);
    props.put("mail.smtp.debug", c.debug);
    props.put("mail.smtp.host", c.host);
    props.put("mail.smtp.pass", c.pass);
    props.put("mail.smtp.port", c.port);
    props.put("mail.smtp.ssl.protocols", c.protocols);
    props.put("mail.smtp.socketFactory.class", c.socketFactory);
    props.put("mail.smtp.starttls.enable", c.starttlsEnabled);
    props.put("mail.smtp.starttls.required", c.starttlsRequired);
    props.put("mail.smtp.user", c.user);

    return c;
  }

  public static void main(String[] args) {
    try {

      Runtime runtime = Runtime.getInstance();
      LoggingFactory.init(Level.INFO);
      Runtime.start("webgui", "WebGui");
      Runtime.start("python", "Python");
      Email email = (Email) Runtime.start("email", "Email");
      // email.setGmailProps("supertick@gmail.com", "XXXXXX");
      // email.save();
      // email.sendImage("supertick@gmail.com", "data/OpenCV/cv-01991.png");
      email.sendEmail(fs, fs, fs, fs);
      email.sendHtmlMail("worky@gmail.com", "supertick@gmail.com", "Test", "Hello there !", "data/OpenCV/cv-01991.png");
      // Runtime.saveConfig("non-default");
      // runtime.save();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
