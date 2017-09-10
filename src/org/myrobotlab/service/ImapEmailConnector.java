package org.myrobotlab.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMultipart;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.document.transformer.ConnectorConfig;
import org.myrobotlab.framework.ServiceType;

import com.sun.mail.imap.IMAPFolder;

/**
 * 
 * ImapEmailConnector - This connector can crawl the folders on an IMAP email
 * server. you can provide the user/pass/email server hostname. It publishes
 * documents that represents the emails messages that were crawled.
 *
 */
public class ImapEmailConnector extends AbstractConnector {

  private static final long serialVersionUID = 1L;
  private static final String MESSAGE_ID_HEADER = "message_id";
  private String emailServer;
  private String username;
  private String password;
  private String folderName = "INBOX";
  private transient String docIdPrefix = "email_";
  private transient Store store;

  public ImapEmailConnector(String name) {
    super(name);
  }

  @Override
  public void setConfig(ConnectorConfig config) {
    // TODO Auto-generated method stub
    log.info("Set Config not yet implemented");
  }

  public void startCrawling() {
    log.info("Sarting IMAP Email connector.");
    // connect to the email store
    Store store = connect();
    if (store == null) {
      log.warn("Email Store was null.  Check credentials and server name");
      return;
    } else {
      log.info("connected to store");
    }
    // Get INBOX folder typically.
    Folder folder = null;
    try {
      folder = store.getFolder(getFolderName());
      folder = openFolder(folder);
    } catch (MessagingException e) {
      log.warn("Folder {} not found.", folder);
      e.printStackTrace();
      return;
    }

    int count = 0;
    try {
      count = processFolder(folder);
      Folder[] folders = folder.list();
      // process all sub folders.
      // TODO: check the recursion here and do it properly.
      for (Folder f : folders) {
        f = openFolder(f);
        count = count + processFolder(f);
      }
    } catch (MessagingException e) {
      log.warn("Message Exception processing subfolders : {}", e.getLocalizedMessage());
      e.printStackTrace();
    }
    disconnect();
    log.info("Fetched " + count + " messages");
  }

  
  public void startListeningForEmail() throws MessagingException {
    // TODO: Implement me and have a publishEmail method.
    Store store = connect();
    
    final IMAPFolder inbox = (IMAPFolder) store.getFolder("inbox");
    inbox.open(Folder.READ_ONLY);

    // TODO: consider moving this into it's own class. 
    inbox.addMessageCountListener(new MessageCountListener() {

        @Override
        public void messagesRemoved(MessageCountEvent event) {
            // NoOp.
        }

        @Override
        public void messagesAdded(MessageCountEvent event) {
            Message[] messages = event.getMessages();
            for (Message message : messages) {
              // a new message arrived, publish it
              invoke("publishEmail", message);
            }
        }
    });

    // a thread to keep our inbox idle open i guess? a a heartbeat perhaps?
    // TODO: rmove this elsewhere?
    new Thread(new Runnable() {
        private static final long KEEP_ALIVE_FREQ = 10000;

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    inbox.idle();
                    Thread.sleep(KEEP_ALIVE_FREQ);                                  
                } catch (InterruptedException e) {
                } catch (MessagingException e) {
                }
            }
        }
    }).start();                 
    
  }
  
  public Message publishEmail(Message m) {
    return m;
  }
  
  
  
  
  private Folder openFolder(Folder folder) {
    try {
      // Read only! lets not accidentally blow away someones email.
      folder.open(Folder.READ_ONLY);
    } catch (MessagingException e) {
      log.info("Message Exception {}", e.getLocalizedMessage());
      e.printStackTrace();
      return null;
    }
    return folder;
  }

  private int processFolder(Folder folder) {
    log.info("Processing folder {}", folder.getName());
    int numDocs = 0;
    try {
      numDocs = folder.getMessageCount();
      log.info("Folder has {} docs.", numDocs);
    } catch (MessagingException e) {
      log.warn("Messaging Exception {}", e.getLocalizedMessage());
      e.printStackTrace();
      // TODO: bomb out here?
      return 0;
    }
    try {
      for (Message m : folder.getMessages()) {
        try {
          Document doc = processMessage(m);
          doc.setField("folder", folder.getName());
          feed(doc);
          numDocs++;
        } catch (MessagingException | IOException e) {
          log.warn("process message failed.  continuing to next message. {} ", e.getLocalizedMessage());
          e.printStackTrace();
          continue;
        }

      }
    } catch (MessagingException e) {
      // TODO Auto-generated catch block
      log.info("Messaging Exception getMessages {}", e.getLocalizedMessage());
      e.printStackTrace();
      return 0;
    }

    return numDocs;
  };

  private Document processMessage(Message m) throws MessagingException, IOException {
    // create a unique(ish) doc id until we discover the true message id.
    String docId = docIdPrefix + UUID.randomUUID().toString();
    Document doc = new Document(docId);
    Enumeration<Header> headers = m.getAllHeaders();
    // walk every header and copy them to fields...
    String messageId = null;
    while (headers.hasMoreElements()) {
      Header header = headers.nextElement();
      String fieldName = cleanFieldName(header.getName());
      if (fieldName.equals(MESSAGE_ID_HEADER)) {
        // if we get a message id. use it.
        messageId = header.getValue();
        docId = docIdPrefix + messageId;
        doc.setId(docId);
      }
      doc.addToField(fieldName, header.getValue());
    }

    // TODO: grab the body of the email
    // TODO: grab the attachments.

    // specific stuff we really care about..
    // We want to map all the From / To / CC / BCCs
    //
    // the "from" field should be handled in the "addHeadersToItem" method.
    //

    // Specially handle the TO field as this is multivalued.
    // not sure which other fields we care about this for.

    // TODO: this might be much faster to call this directly.. just need to
    // pass it
    // the header that we already copied to the to / bcc /cc fields of the
    // mime message.
    // InternetAddress.parseHeader(toHeader, this.strict)
    //
    // Address[] recipients = m.getAllRecipients();

    // TODO: i don't like calling toString here.
    if (doc.hasField("to")) {
      // if the to field was found, we are going to override it here.
      Address[] recipients = InternetAddress.parse(doc.getField("to").toString());
      if (recipients != null) {
        doc.removeField("to");
        for (Address a : recipients) {
          doc.addToField("to", a.toString());
        }
      } else {
        // this shouldn't happen, right?
        doc.setField("to", "unknown");
      }
    } else {
      // this shouldn't happen?
      doc.setField("to", "unknown");
    }

    // TODO: what to use with the sent date?
    // Date d = m.getSentDate();
    Date sentdate = null;
    // TODO: make it so we don't call tostring here.
    // TODO: array out of bounds checking...
    if (!doc.hasField("date")) {
      sentdate = m.getSentDate();
      doc.setField("sent_date", sentdate);
    } else {
      MailDateFormat mailDateFormat = new MailDateFormat();
      try {
        // parse the string version of the field and make it a proper
        // java date object
        sentdate = mailDateFormat.parse(doc.getField("date").get(0).toString());
        doc.setField("sent_date", sentdate);
      } catch (ParseException e) {
        log.warn("Date Parse Exception {}", e.getLocalizedMessage());
        e.printStackTrace();
      }
    }

    Date receivedDate = m.getReceivedDate();
    if (receivedDate != null) {
      doc.setField("received_date", receivedDate);
    }

    Address[] replyTo = m.getReplyTo();
    if (replyTo != null) {
      for (Address replyAddr : replyTo) {
        doc.addToField("reply_to", replyAddr.toString());
      }
    }

    String subject = m.getSubject();
    if (subject != null) {
      doc.setField("subject", subject);
    } else {
      log.debug("No subject");
    }

    // the body of the email here
    Object content = m.getContent();
    if (content instanceof String) {
      // This is already a string! ok...
      doc.addToField("text", (String) (content));
    } else if (content instanceof MimeMultipart) {
      // multi-part mime docs are a pain. we'll just accumulate the
      // text from each part.
      int numParts = ((MimeMultipart) content).getCount();
      // Walk all parts of the mime message.
      for (int i = 0; i < numParts; i++) {
        BodyPart bp = ((MimeMultipart) content).getBodyPart(i);
        // add the various metadata fields to the document for this body
        // part.
        try {
          parseBodyPart(bp, doc);
        } catch (Exception e) {
          log.warn("Exception in parse body part for message {}", e.getLocalizedMessage());
          e.printStackTrace();
          continue;
        }
      }
    } else {
      log.info("Unknown Type of content returned : " + content.getClass());
      doc.addToField("text", content.toString());
    }
    doc.setField("size", m.getSize());
    return doc;
  }

  public void parseBodyPart(Part p, Document doc) throws Exception {
    //
    // switch on ismimetype for processing. (avoid fetching if we don't need
    // to!)
    // attachments can be large.
    if (p.isMimeType("text/plain")) {
      String body = (String) p.getContent();
      doc.addToField("text", body);
      return;
    } else if (p.isMimeType("multipart/alternative")) {
      MimeMultipart mmp = (MimeMultipart) p.getContent();
      for (int i = 0; i < mmp.getCount(); i++) {
        // TODO: check this recursion! nested body parts!
        parseBodyPart(mmp.getBodyPart(i), doc);
      }
      return;
    } else if (p.isMimeType("text/html")) {
      String body = (String) p.getContent();
      // TODO: have the pipeline parse the html
      doc.addToField("html", body);
      return;
    } else if (p.isMimeType("application/ics")) {
      log.info("Skipping Calender entry: not supported yet.");
      Object icsEntry = p.getContent();
      // TODO: add this to the doc
      doc.addToField("ics", icsEntry.toString());
      return;
    } else {
      log.info("Unhandled Content Type {}", p.getContentType());
      return;
    }
  }

  private String cleanFieldName(String name) {
    // TODO : centralize this as a util or something. (maybe move it to the
    // pipeline)
    String clean = name.trim().toLowerCase().replaceAll(" ", "_");
    return clean;
  }

  @Override
  public void stopCrawling() {
    // TODO Auto-generated method stub
    // TODO: this isn't implemented yet.. I'd like to move this sort of
    // stuff to the base class.
  }

  public void disconnect() {
    try {
      store.close();
    } catch (MessagingException e) {
      log.warn("error closing store ... " + e.getMessage());
      e.printStackTrace();
    }
  }

  public Store connect() {
    Properties props = System.getProperties();
    props.setProperty("mail.store.protocol", "imaps");
    Session session = Session.getDefaultInstance(props, null);
    Store store = null;
    try {
      store = session.getStore("imaps");
    } catch (NoSuchProviderException e) {
      log.warn("No IMAPS support. {}", e.getLocalizedMessage());
      e.printStackTrace();
    }
    try {
      store.connect(getEmailServer(), getUsername(), getPassword());
    } catch (MessagingException e) {
      // TODO Auto-generated catch block
      log.warn(e.getMessage());
      e.printStackTrace();
      return null;
    }
    return store;
  }

  public String getEmailServer() {
    return emailServer;
  }

  public void setEmailServer(String emailServer) {
    this.emailServer = emailServer;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFolderName() {
    return folderName;
  }

  public void setFolderName(String folderName) {
    this.folderName = folderName;
  }

  public String getDocIdPrefix() {
    return docIdPrefix;
  }

  public void setDocIdPrefix(String docIdPrefix) {
    this.docIdPrefix = docIdPrefix;
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

    ServiceType meta = new ServiceType(ImapEmailConnector.class.getCanonicalName());
    meta.addDescription("This connector will connect to an IMAP based email server and crawl the emails");
    meta.addCategory("data", "ingest");
    meta.addDependency("com.sun.mail", "1.4.5");
    meta.setCloudService(true);

    return meta;
  }

  
  public static void main(String[] args) throws Exception {
    ImapEmailConnector connector = (ImapEmailConnector) Runtime.start("email", "ImapEmailConnector");
    connector.setEmailServer("imap.gmail.com");
    connector.setUsername("XX");
    connector.setPassword("YY");
    connector.setBatchSize(1);
    //Solr solr = (Solr) Runtime.start("solr", "Solr");
    // for example...
    //String solrUrl = "http://phobos:8983/solr/collection1";
    //solr.setSolrUrl(solrUrl);
    //connector.addDocumentListener(solr);
    //connector.startCrawling();
    
    connector.startListeningForEmail();
    
    
  }
  
}
