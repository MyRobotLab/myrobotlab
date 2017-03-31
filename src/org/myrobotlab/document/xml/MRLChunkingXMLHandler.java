package org.myrobotlab.document.xml;

import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.myrobotlab.document.Document;
import org.myrobotlab.document.connector.AbstractConnector;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class MRLChunkingXMLHandler implements ContentHandler {

  transient public final static Logger log = LoggerFactory.getLogger(Service.class);

  Stack<String> currentPath = new Stack<String>();
  private AbstractConnector connector;
  private String documentRootPath;
  private String documentIDPath;
  private String docIDPrefix = "";
  private boolean inDocID = false;
  // private boolean inDoc = false;
  private StringBuilder docIDBuilder = new StringBuilder();
  private RecordingInputStream ris;

  @Override
  public void setDocumentLocator(Locator locator) {
    // TODO Auto-generated method stub
  }

  @Override
  public void startDocument() throws SAXException {
    // TODO Auto-generated method stub

  }

  @Override
  public void endDocument() throws SAXException {
    // TODO Auto-generated method stub

  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    // TODO Auto-generated method stub

  }

  @Override
  public void endPrefixMapping(String prefix) throws SAXException {
    // TODO Auto-generated method stub

  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    //

    // push on the stack.
    currentPath.push(qName);
    // log.info("Start element: {}",qName);
    String path = "/" + StringUtils.join(currentPath.toArray(), "/");
    if (documentRootPath.equals(path)) {
      // this is the start of our page.
      // inDoc = true;
      docIDBuilder = new StringBuilder();
      // ok we should clear our input buffer up to the current offset for this
      // start element.
      try {
        ris.clearUpTo("<" + qName);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    if (documentIDPath.equals(path)) {
      // this is the start of the document id field.
      inDocID = true;
    }
    // append to the current current page the tag and it's attributes.

    // TODO: properly encode/escape these!! could
    // cause xml parsing errors!?! eek.
    // for (int i = 0; i<atts.getLength(); i++) {
    // StringBuilder attrBuilder = new StringBuilder();
    // attrBuilder.append(" ");
    // attrBuilder.append(atts.getQName(i));
    // attrBuilder.append("=\"");
    // attrBuilder.append(atts.getValue(i));
    // attrBuilder.append("\"");
    // pageBuffer.append(attrBuilder.toString());
    // }

  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    // we just finished a path. see if it's the doc root that we're looking for.
    String path = "/" + StringUtils.join(currentPath.toArray(), "/");
    if (documentRootPath.equals(path)) {

      // ok, now we want the buffer up to the close tag.
      String xml = "Malformed";
      try {
        xml = ris.returnUpTo("</" + qName + ">");
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      // System.out.println("------------------------------------");
      // System.out.println(xml);
      // System.out.println("------------------------------------");

      // this is the end of our page send the buffer as a document
      Document doc = new Document(docIDPrefix + docIDBuilder.toString());
      // doc.setField("xml", pageBuffer.toString());
      doc.setField("xml", xml);
      internalPublishDocument(doc);
    }
    if (documentIDPath.equals(path)) {
      // this is the end of the doc id tag.
      inDocID = false;
    }
    // pop up..
    currentPath.pop();
    // System.out.println(path);
  }

  private void internalPublishDocument(Document doc) {
    // publish the doc.
    // does this need to be an invoke?
    // always feed a batch
    connector.feed(doc);
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (inDocID) {
      docIDBuilder.append(Arrays.copyOfRange(ch, start, start + length));
    }
  }

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    // TODO Auto-generated method stub

  }

  @Override
  public void processingInstruction(String target, String data) throws SAXException {
    // TODO Auto-generated method stub

  }

  @Override
  public void skippedEntity(String name) throws SAXException {
    // TODO Auto-generated method stub
  }

  public String getDocumentRootPath() {
    return documentRootPath;
  }

  public void setDocumentRootPath(String documentRootPath) {
    this.documentRootPath = documentRootPath;
  }

  public String getDocumentIDPath() {
    return documentIDPath;
  }

  public void setDocumentIDPath(String documentIDPath) {
    this.documentIDPath = documentIDPath;
  }

  public String getDocIDPrefix() {
    return docIDPrefix;
  }

  public void setDocIDPrefix(String docIDPrefix) {
    this.docIDPrefix = docIDPrefix;
  }

  public void setConnector(AbstractConnector connector) {
    this.connector = connector;
  }

  public RecordingInputStream getRis() {
    return ris;
  }

  public void setRis(RecordingInputStream ris) {
    this.ris = ris;
  }

}
