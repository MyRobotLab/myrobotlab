package org.myrobotlab.service.data;

import java.util.ArrayList;
import java.util.List;

// FIXME me have a more universal description of SearchResults
// urls ? confidence ?
public class SearchResults {

  public String searchText;
  public String errorText;
  public List<String> text = new ArrayList<>();
  public List<String> images = new ArrayList<>();

  public SearchResults(String searchText) {
    this.searchText = searchText;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (String t : text) {
      sb.append(t);
    }
    return sb.toString();
  }

  public String getText() {
    StringBuilder sb = new StringBuilder();
    for (String t : text) {
      sb.append(t);
    }
    return sb.toString();
  }
}
