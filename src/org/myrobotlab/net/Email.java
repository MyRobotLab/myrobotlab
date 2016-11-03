package org.myrobotlab.net;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         General email dependencies are mailapi.jar and smtp.jar
 * 
 */
public class Email {

  public final static Logger log = LoggerFactory.getLogger(Email.class);

  public final static String FORMAT_HTML = "text/html";
  public final static String FORMAT_TEXT = "text/plain";

  Properties emailProperties;
  Session mailSession;

  public static void main(String args[]) throws AddressException, MessagingException {
    try {

      LoggingFactory.init(Level.ERROR);

      Email email = new Email();
      // email.setGmailServer();
      email.setEmailServer("mail.freightliner.com");
      // email.createEmailMessage("greg.perry@daimler.com", "test",
      // "test body");
      email.sendEmail("greg.perry@daimler.com", "test", "test body");
      // email.sendEmailWithImage("greg.perry@daimler.com", "test",
      // "test body", "opencv.input.4.jpg");

      log.info("done");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public MimeMessage createEmailMessage(String to, String subject, String body) throws AddressException, MessagingException {
    return createEmailMessage(to, subject, body, FORMAT_TEXT);
  }

  public MimeMessage createEmailMessage(String to, String subject, String body, String format) throws AddressException, MessagingException {
    return createEmailMessage(new String[] { to }, subject, body, format);
  }

  public MimeMessage createEmailMessage(String[] to, String subject, String body) throws AddressException, MessagingException {
    return createEmailMessage(to, subject, body, FORMAT_TEXT);
  }

  public MimeMessage createEmailMessage(String[] to, String subject, String body, String format) throws AddressException, MessagingException {
    mailSession = Session.getDefaultInstance(emailProperties, null);
    // mailSession = Session.getInstance(emailProperties, null);

    MimeMessage msg = new MimeMessage(mailSession);

    for (int i = 0; i < to.length; i++) {
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to[i]));
    }

    msg.setSubject(subject);
    msg.setContent(body, format);

    return msg;
  }

  // TODO String[] of attachments - Derive mimeType from File - inline with
  // img if is image
  public MimeMessage createEmailMessageWithImage(String to, String subject, String body, String imgFileName) throws AddressException, MessagingException {

    MimeMessage msg = createEmailMessage(to, subject, String.format("%s <br/>\n <img src=\"%s\"/>", body, imgFileName));

    MimeBodyPart messageBodyPart = new MimeBodyPart();

    messageBodyPart.setContent(String.format("%s <br/>\n <img src=\"%s\"/>", body, imgFileName), "text/html");

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(messageBodyPart);

    messageBodyPart = new MimeBodyPart();
    File img = new File(imgFileName);
    DataSource source = new FileDataSource(img);
    messageBodyPart.setDataHandler(new DataHandler(source));
    messageBodyPart.setFileName(imgFileName);
    messageBodyPart.setDisposition(Part.INLINE);
    multipart.addBodyPart(messageBodyPart);

    msg.setContent(multipart);
    return msg;
  }

  public void sendEmail(MimeMessage msg) throws AddressException, MessagingException {

    Transport transport = mailSession.getTransport("smtp");

    transport.connect();
    // transport.connect(emailHost, fromUser, fromUserEmailPassword);
    transport.sendMessage(msg, msg.getAllRecipients());
    transport.close();
    log.info("Email sent successfully.");
  }

  public void sendEmail(String to, String subject, String body) throws AddressException, MessagingException {

    MimeMessage msg = createEmailMessage(to, subject, body);
    Transport transport = mailSession.getTransport("smtp");

    transport.connect();
    // transport.connect(emailHost, fromUser, fromUserEmailPassword);
    transport.sendMessage(msg, msg.getAllRecipients());
    transport.close();
    log.info("Email sent successfully.");
  }

  public void sendEmail(String[] to, String subject, String body) throws AddressException, MessagingException {
    for (int i = 0; i < to.length; ++i) {
      sendEmail(to[i], subject, body);
    }

  }

  // FIXME - needs work generalize to take a File[] and extract mime info
  public void sendEmailWithImage(String to, String subject, String body, String imgFileName) throws AddressException, MessagingException {

    MimeMessage msg = createEmailMessageWithImage(to, subject, body, imgFileName);
    Transport transport = mailSession.getTransport("smtp");

    transport.connect();
    // transport.connect(emailHost, fromUser, fromUserEmailPassword);
    transport.sendMessage(msg, msg.getAllRecipients());
    transport.close();
    log.info("Email sent successfully.");
  }

  public void setEmailServer(String host) {
    // docs of all email properties
    // https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html
    emailProperties = System.getProperties();
    emailProperties.put("mail.smtp.host", host);
    emailProperties.put("mail.smtp.port", 25);
    // emailProperties.put("mail.smtp.auth", "false");
    // emailProperties.put("mail.smtp.starttls.enable", "true");
  }

  public void setEmailServer(String host, Integer port) {
    emailProperties = System.getProperties();
    emailProperties.put("mail.smtp.host", host);
    emailProperties.put("mail.smtp.port", port);
    // emailProperties.put("mail.smtp.auth", "true");
    // emailProperties.put("mail.smtp.starttls.enable", "true");
  }

  public void setGmailServer(String user, String password) {
    emailProperties = System.getProperties();
    // gmail's smtp port
    emailProperties.put("mail.smtp.user", user);
    emailProperties.put("mail.smtp.pass", password);
    emailProperties.put("mail.smtp.host", "smtp.gmail.com");
    emailProperties.put("mail.smtp.port", "587");
    emailProperties.put("mail.smtp.auth", "true");
    emailProperties.put("mail.smtp.starttls.enable", "true");
  }

}