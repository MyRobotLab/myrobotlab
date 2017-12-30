package org.myrobotlab.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.document.connector.ConnectorState;
import org.myrobotlab.document.transformer.ConnectorConfig;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.string.StringUtil;

import au.com.bytecode.opencsv.CSVReader;

public class CsvConnector extends AbstractConnector {

  private static final long serialVersionUID = 1L;
  private String filename;
  private String[] columns;
  private String idField;
  private String separator = ",";
  private int numFields;
  private int idColumn = -1;
  private boolean useRowAsId = true;
  private int skipRows = 1;
  private boolean firstRowAsColumns = false;

  public CsvConnector(String name) {
    super(name);
  }

  @Override
  public void setConfig(ConnectorConfig config) {
    // TODO: remove side effects of a "setter"
    // the parsing of the config should be handled elsewhere? maybe initialize?
    // TODO: validate the config options are valid.

    setDocIdPrefix(config.getStringParam("docIdPrefix", ""));
    filename = config.getProperty("filename");
    columns = config.getStringArray("columns");
    idField = config.getProperty("idField");

    separator = config.getProperty("separator");
    numFields = config.getIntegerParam("numFields", numFields);
    // this is computed in initialize.
    // idColumn = config.getProperty("idColumn");
    useRowAsId = config.getBoolParam("useRowAsId", useRowAsId);
    skipRows = config.getIntegerParam("skipRows", skipRows);
    firstRowAsColumns = config.getBoolParam("firstRowAsColumns", firstRowAsColumns);
  }

  public void initialize() {
    // filename = config.getProperty("filename", "data/myfile.csv");
    // columns = config.getProperty("columnnames",
    // "id,column1,column2").split(",");
    // idField = config.getProperty("idcolumn", "id");
    // idPrefix = config.getProperty("idprefix", "doc_");
    // separator = config.getProperty("separator", ",");
    // if (separator.length() > 1) {
    // // This is an error condition we can only have a character as a
    // separator.
    //
    // }

    numFields = columns.length;
    for (int i = 0; i < numFields; i++) {
      if (columns[i].equals(idField)) {
        idColumn = i;
        break;
      }
    }
  }

  @Override
  public void startCrawling() {

    state = ConnectorState.RUNNING;
    // compile the map to for header to column number.
    // TODO: add a directory traversal ..
    // log.info("Starting CSV Connector");
    File fileToCrawl = new File(filename);
    if (!fileToCrawl.exists()) {
      // error. file not found.
      error("File not found..." + filename);
      return;
    }

    FileReader reader = null;
    try {
      reader = new FileReader(fileToCrawl);
    } catch (FileNotFoundException e) {
      // This should not happen
      e.printStackTrace();
    }
    CSVReader csvReader = new CSVReader(reader, separator.charAt(0));
    if (firstRowAsColumns) {
      // we should read the first row as the column header
      try {
        columns = csvReader.readNext();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    // pick out which column has the primary key / id field.
    initialize();

    int rowNum = 0;
    String[] nextLine;

    try {

      while ((nextLine = csvReader.readNext()) != null) {
        // TODO: replace this with connector state, and make private isRunning
        // again.
        if (!state.equals(ConnectorState.RUNNING)) {
          // we've been interrupted.
          log.info("Crawl interrupted, stopping crawl.");
          state = ConnectorState.INTERRUPTED;
          break;
        }
        rowNum++;
        if (rowNum <= skipRows) {
          continue;
        }
        String id;
        if (useRowAsId) {
          id = getDocIdPrefix() + rowNum;
        } else {
          id = getDocIdPrefix() + nextLine[idColumn];
        }
        Document docToSend = new Document(id);
        for (int i = 0; i < numFields; i++) {
          String v = nextLine[i];
          if (!StringUtil.isEmpty(v)) {
            docToSend.addToField(columns[i], v);
          }
        }
        feed(docToSend);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      // shouldn't see this.. but who knows.
      e.printStackTrace();
      log.error("IO Exception during crawl. {}", e.getMessage());
      // TODO: re-throw something else?
    }

    // Lets poll until our outbox has been completely picked up.
    while (outbox.size() > 0) {
      // wait until our outbox has drained before going to stopped?
      try {
        log.info("Waiting for outbox to drain. Size: {}", outbox.size());
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    // TODO: why the heck does this not block until we're done as we expect?!?!
    state = ConnectorState.STOPPED;
    flush();
    // TODO: push this state management to the base class?

  }

  @Override
  public void stopCrawling() {
    flush();
    state = ConnectorState.STOPPED;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String[] getColumns() {
    return columns;
  }

  public void setColumns(String... columns) {
    this.columns = columns;
  }

  public String getIdField() {
    return idField;
  }

  public void setIdField(String idField) {
    this.idField = idField;
  }

  public String getSeparator() {
    return separator;
  }

  public void setSeparator(String separator) {
    this.separator = separator;
  }

  public int getNumFields() {
    return numFields;
  }

  public void setNumFields(int numFields) {
    this.numFields = numFields;
  }

  public int getIdColumn() {
    return idColumn;
  }

  public void setIdColumn(int idColumn) {
    this.idColumn = idColumn;
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

    ServiceType meta = new ServiceType(CsvConnector.class.getCanonicalName());
    meta.addDescription("This service crawls a csv file and publishes each row as a document");
    meta.addCategory("ingest");
    meta.addDependency("net.sf.opencsv", "2.3");
    return meta;
  }

}
