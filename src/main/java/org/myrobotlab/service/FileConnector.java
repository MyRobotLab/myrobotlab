package org.myrobotlab.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.document.transformer.ConnectorConfig;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.FileConnectorConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.DocumentPublisher;
import org.slf4j.Logger;

public class FileConnector extends AbstractConnector implements DocumentPublisher, FileVisitor<Path> {

  public final static Logger log = LoggerFactory.getLogger(FileConnector.class.getCanonicalName());
  private static final long serialVersionUID = 1L;
  // private String directory;
  private FileConnectorConfig config = new FileConnectorConfig();
  // TODO: add wildcard includes/excludes
  // TODO: add file path includes/excludes
  private volatile boolean interrupted = false;

  public FileConnector(String name, String id) {
    super(name, id);
  }

  @Override
  public void setConfig(ConnectorConfig config) {
    // TODO Auto-generated method stub
    log.info("Set Config not yet implemented");
  }

  @Override
  public void startCrawling() {
    state = ConnectorState.RUNNING;
    Path startPath = Paths.get(((FileConnectorConfig)config).directory);
    log.info("Started Crawling {}", startPath);
    try {
      Files.walkFileTree(startPath, this);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // we're done.. publish a flush so other down stream components know to flush any partial batches they might have.
    invoke("publishFlush");
    log.info("File Connector finished walking the tree.");
    
    // TODO: should we flush here immediately?
    state = ConnectorState.STOPPED;
  }

  @Override
  public void stopCrawling() {
    log.info("Stop crawling requested...");    
    interrupted = true;
    state = ConnectorState.INTERRUPTED;
//    notify();
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    if (interrupted) {
      state = ConnectorState.INTERRUPTED;
      return FileVisitResult.TERMINATE;
    }
    String docId = getDocIdPrefix() + file.toFile().getAbsolutePath();
    Document doc = new Document(docId);
    doc.setField("last_modified", new Date(attrs.lastModifiedTime().toMillis()));
    doc.setField("created_date", new Date(attrs.creationTime().toMillis()));
    doc.setField("filepath", file.toFile().getAbsolutePath());
    doc.setField("size", attrs.size());
    doc.setField("type", "file");
    // TODO: potentially add a byte array of the file
    // or maybe an input stream or other handle to the file.
    feed(doc);
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    if (interrupted) {
      state = ConnectorState.INTERRUPTED;
      return FileVisitResult.TERMINATE;
    }
    String docId = getDocIdPrefix() + file.toFile().getAbsolutePath();
    Document doc = new Document(docId);
    doc.setField("type", "file");
    // TODO: how does this serialize?
    doc.setField("error", exc);
    // doc.setField("timestamp", new Date());
    feed(doc);
    log.warn("Exception processing {}", file, exc);
    // Keep going!!!
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    if (exc != null) {
      throw exc;
    }
    return FileVisitResult.CONTINUE;
  }

  public String getDirectory() {
    return config.directory;
  }

  public void setDirectory(String directory) {
    config.directory = directory;
  }

  @Override
  public ServiceConfig apply(ServiceConfig inConfig) {
    // 
    FileConnectorConfig config = (FileConnectorConfig)super.apply(inConfig);
    // anything else?
    return config;
  }

  @Override
  public ServiceConfig getConfig() {
    // return the config
    // we need the super stuff here.
    FileConnectorConfig config = (FileConnectorConfig)super.getConfig();
    // this is goofy..
    config.directory = this.config.directory;
    return config;
  }
}
