package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.document.Document;
import org.myrobotlab.document.ProcessingStatus;
import org.myrobotlab.framework.Service;

public class MockDocumentListener extends Service implements DocumentListener {

  private static final long serialVersionUID = 1L;
  private int count = 0;

  public MockDocumentListener(String n, String id) {
    super(n, id);
    this.inbox.setBlocking(true);

  }

  public String[] getCategories() {
    // TODO Auto-generated method stub
    return new String[] { "testing" };
  }

  public int getCount() {
    return count;
  }

  @Override
  public String getDescription() {
    return "A Mock document listener for testing";
  }

  @Override
  public String getName() {
    return super.getName();
  }

  @Override
  public ProcessingStatus onDocument(Document doc) {
    count++;
    System.out.println(doc.getId());
    return ProcessingStatus.OK;
  }

  @Override
  public ProcessingStatus onDocuments(List<Document> docs) {
    // TODO Auto-generated method stub
    count += docs.size();
    return ProcessingStatus.OK;
  }

  @Override
  public boolean onFlush() {
    // TODO Auto-generated method stub
    return false;
  }

}
