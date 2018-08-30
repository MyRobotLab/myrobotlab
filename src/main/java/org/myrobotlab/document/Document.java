package org.myrobotlab.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * The basic class that represents a document flowing through the myrobotlab.
 * 
 * Basic idea is that a document had a unique id and a map of key to list of
 * object pairs.
 * 
 * @author kwatters
 *
 */
public class Document {

  private String id;
  private HashMap<String, ArrayList<Object>> data;
  private ProcessingStatus status;

  public Document(String id) {
    this.id = id;
    data = new HashMap<String, ArrayList<Object>>();
    status = ProcessingStatus.OK;
  }

  public ArrayList<Object> getField(String fieldName) {
    if (data.containsKey(fieldName)) {
      return data.get(fieldName);
    } else {
      return null;
    }
  }

  public void setField(String fieldName, ArrayList<Object> value) {
    if (value == null) {
      data.remove(fieldName);
    } else {
      data.put(fieldName, value);  
    }
  }

  public void setField(String fieldName, Object value) {
    // set field overwrites existing values in the field.
    if (value == null) {
      data.remove(fieldName);
    } else {
      ArrayList<Object> values = new ArrayList<Object>();
      values.add(value);
      data.put(fieldName, values);
    }
  }

  public void renameField(String oldField, String newField) {
    if (data.containsKey(oldField)) {
      // TODO: test me to make sure this is correct.
      data.put(newField, data.get(oldField));
      data.remove(oldField);
    }
  }

  public void addToField(String fieldName, Object value) {
    if (data.containsKey(fieldName) && (data.get(fieldName) != null)) {
      data.get(fieldName).add(value);
    } else {
      ArrayList<Object> values = new ArrayList<Object>();
      values.add(value);
      data.put(fieldName, values);
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean hasField(String fieldName) {
    return data.containsKey(fieldName);
  }

  /**
   * Return a set of all fields on a given document. This is unordered.
   * 
   * @return a list of all field names that have been set of the document.
   */
  public Set<String> getFields() {
    // TODO Auto-generated method stub
    return data.keySet();
    // return null;
  }

  public void removeField(String fieldName) {
    data.remove(fieldName);

  }

  public ProcessingStatus getStatus() {
    return status;
  }

  public void setStatus(ProcessingStatus status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((data == null) ? 0 : data.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((status == null) ? 0 : status.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Document other = (Document) obj;
    if (data == null) {
      if (other.data != null)
        return false;
    } else if (!data.equals(other.data))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (status != other.status)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Document [id=" + id + ", data=" + data + ", status=" + status + "]";
  }

}
