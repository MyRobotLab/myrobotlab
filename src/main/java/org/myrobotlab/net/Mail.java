package org.myrobotlab.net;

import java.io.File;
import java.util.Date;
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
public class Mail {

  public final static Logger log = LoggerFactory.getLogger(Mail.class);

  public final static String FORMAT_HTML = "text/html";
  public final static String FORMAT_TEXT = "text/plain";

  Properties props;
  Session mailSession;

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
    mailSession = Session.getDefaultInstance(props);
    // , new javax.mail.Authenticator() {
    // protected PasswordAuthentication getPasswordAuthentication() {
    // return new PasswordAuthentication("zzzz", "xxxx");
    // }
    // }
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
    Transport transport = mailSession.getTransport("smtps");

    transport.connect("xxxx", "zzzz");
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
    props = System.getProperties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", 25);
    // emailProperties.put("mail.smtp.auth", "false");
    // emailProperties.put("mail.smtp.starttls.enable", "true");
  }

  public void setEmailServer(String host, Integer port) {
    props = System.getProperties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);
    // emailProperties.put("mail.smtp.auth", "true");
    // emailProperties.put("mail.smtp.starttls.enable", "true");
  }

  /**
   * This will work with gmail but an "app password" will need to be set up on
   * the sending account.
   * 
   * "Create and use App Passwords"
   * https://support.google.com/mail/answer/185833?hl=en
   * 
   * @param host
   * @param port
   * @param userName
   * @param password
   * @param toAddress
   * @param subject
   * @param message
   * @throws AddressException
   * @throws MessagingException
   */
  public void sendPlainTextEmail(String host, String port, final String userName, final String password, String toAddress, String subject, String message)
      throws AddressException, MessagingException {

    // sets SMTP server properties
    Properties props = new Properties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.user", userName);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
    props.put("mail.smtp.debug", "true");

    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587");// "465"
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");
    props.put("mail.smtp.ssl.protocols", "TLSv1.2");
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

    Session session = Session.getDefaultInstance(props);
    session.setDebug(true);

    // creates a new e-mail message
    Message msg = new MimeMessage(session);

    msg.setFrom(new InternetAddress(userName));
    InternetAddress[] toAddresses = { new InternetAddress(toAddress) };
    msg.setRecipients(Message.RecipientType.TO, toAddresses);
    msg.setSubject(subject);
    msg.setSentDate(new Date());
    msg.setText(message);

    Transport t = session.getTransport("smtp");
    // t.connect(host, userName, password);
    t.connect(host, userName, password);
    t.sendMessage(msg, msg.getAllRecipients());
    t.close();

  }

  public void setGmailServer(String user, String password) {
    props = System.getProperties();
    // gmail's smtp port
    props.put("mail.smtp.user", user);
    props.put("mail.smtp.pass", password);
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.debug", "true");
    // props.put("mail.smtp.auth", "true"); // If you need to authenticate
    // Use the following if you need SSL
    // props.put("mail.smtp.socketFactory.port", "587");
    // props.put("mail.smtp.socketFactory.class",
    // "javax.net.ssl.SSLSocketFactory");
    // props.put("mail.smtp.socketFactory.fallback", "false");
  }

  public static void main(String args[]) throws AddressException, MessagingException {
    try {

      LoggingFactory.init(Level.DEBUG);

      Mail email = new Mail();
      email.sendPlainTextEmail("smtp.gmail.com", "587", "username@gmail.com", "app-password-xxxxxxxxxx", "grog@myrobotlab.org", "test", "test");
      // email.setGmailServer();
      // email.setEmailServer("smtp-relay.gmail.com");
      email.setGmailServer("yyyy", "xxxxxxx");
      // email.createEmailMessage("greg.perry@daimler.com", "test",
      // "test body");
      email.sendEmail("yyyy", "test2", "test body2");
      // email.sendEmailWithImage("greg.perry@daimler.com", "test",
      // "test body", "opencv.input.4.jpg");

      log.info("done");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}