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
import org.myrobotlab.service.interfaces.DocumentPublisher;
import org.slf4j.Logger;

public class FileConnector extends AbstractConnector implements DocumentPublisher, FileVisitor<Path> {

  public final static Logger log = LoggerFactory.getLogger(FileConnector.class.getCanonicalName());
  private static final long serialVersionUID = 1L;
  private String directory;
  // TODO: add wildcard includes/excludes
  // TODO: add file path includes/excludes
  private boolean interrupted = false;

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
    Path startPath = Paths.get(directory);
    try {
      Files.walkFileTree(startPath, this);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    log.info("File Connector finished walking the tree.");
    // TODO: should we flush here immediately?
    state = ConnectorState.STOPPED;
  }

  @Override
  public void stopCrawling() {
    interrupted = true;
    state = ConnectorState.INTERRUPTED;
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
    // throw exc;
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
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

}
