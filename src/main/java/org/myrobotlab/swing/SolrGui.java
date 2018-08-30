package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class SolrGui extends ServiceGui implements ActionListener {

  private String boundServiceName;
  private JTextField queryStringBox = new JTextField("*:*", 30);
  private JTextArea results = new JTextArea("Search Results:");
 // private JScrollPane scrollResponse = new JScrollPane(results);
  private JButton searchButton = new JButton("Search");
  
  
  private JTextArea facetResults = new JTextArea("Facet Results:");
  
  private JLabel resultInfoLine = new JLabel();
  
  public SolrGui(String boundServiceName, SwingGui myService) {
    super(boundServiceName, myService);
    // search box.
    this.boundServiceName = boundServiceName;
    //int fontSize =  14;
    
    
    display.setLayout(new BorderLayout());
    createSearchGuiLayout();
    
    //
//    scrollResponse.setAutoscrolls(true);
//    
//    JPanel responseInfoPanel = new JPanel();
//    //responseInfoPanel.add(resultInfoLine);
//    responseInfoPanel.add(scrollResponse, BorderLayout.PAGE_START);
//    
//    display.add(responseInfoPanel, BorderLayout.LINE_START);
////    display.add(responseInfoPanel, BorderLayout.LINE_START);
//    //display.add(scrollResponse, BorderLayout.CENTER);

    // attach callbacks
    searchButton.addActionListener(this);
    queryStringBox.addActionListener(this);
    
    
  }
  
  private void createSearchGuiLayout() {

    // create the search bar
    int fontSize =  20;
    queryStringBox.setFont(new Font("Arial", Font.BOLD, fontSize));
    queryStringBox.setPreferredSize(new Dimension(40, 35));

    // create the search button
    JPanel searchBar = new JPanel();
    searchBar.setLayout(new BorderLayout());
    searchBar.add(queryStringBox, BorderLayout.LINE_START);
    searchBar.add(searchButton, BorderLayout.LINE_END);

    JScrollPane scrollResponse = new JScrollPane(results);
    // now create the search result area
    scrollResponse.setAutoscrolls(true);
    
    
    JLabel footer = new JLabel("end..");
    
    JPanel resultArea = new JPanel();
    resultArea.setLayout(new BorderLayout());
    resultArea.add(resultInfoLine, BorderLayout.NORTH);
    resultArea.add(facetResults, BorderLayout.WEST);
    resultArea.add(scrollResponse, BorderLayout.CENTER);
    resultArea.add(footer, BorderLayout.SOUTH);
    
    // add all the parts to the display 
    display.add(searchBar, BorderLayout.NORTH);
    display.add(resultArea, BorderLayout.CENTER);
    
  }
  
  public final static Logger log = LoggerFactory.getLogger(SolrGui.class.toString());
  static final long serialVersionUID = 1L;
  
  
  @Override
  public void actionPerformed(ActionEvent event) {
    // TODO Auto-generated method stub
    Object o = event.getSource();
    // TODO: stuff.
    if (o == searchButton || o == queryStringBox) {
      // Run a new search and render the result.
      runSearchAndRender();
    }
  }

  private void runSearchAndRender() {
    SolrQuery query = createSearchRequest();
    // 10 second timeout!
    QueryResponse answer = (QueryResponse) swingGui.sendBlocking(boundServiceName, 10000, "search", query);
    // TODO: build up a search result page.
    renderSearchResult(answer);
  }


  private void renderSearchResult(QueryResponse answer) {
    
    long start = answer.getResults().getStart();
    long end = answer.getResults().size() + start;
    long numFound = answer.getResults().getNumFound();
    int duration = answer.getQTime();
    
    resultInfoLine.setText(String.format("Displaying hit number %s to %s of %s in %s milliseconds", start, end, numFound, duration));
    
    
    facetResults.setText("");
    StringBuilder facetResultBuilder = new StringBuilder();
    for (FacetField f : answer.getFacetFields()) {
      facetResultBuilder.append("\n------------------\n"+f.getName() + "\n");
      for (Count v : f.getValues()) {
        facetResultBuilder.append(v.getName() + " ");
        facetResultBuilder.append(v.getCount() + "\n");
      }
    }
    facetResults.setText(facetResultBuilder.toString());
    
    results.setText("");
    for (SolrDocument d : answer.getResults()) {  
      results.append("################################\n");
      for (String fieldName : d.getFieldNames()) {
        results.append(fieldName +":");
        int i = 0;
        for (Object val : d.getFieldValues(fieldName)) {
          i++;
          results.append(val.toString());
          if (i < d.getFieldValues(fieldName).size()) {
            results.append(", ");
          }
        }
        results.append("\n");
      }
    }
    results.setCaretPosition(0);
  }


  private SolrQuery createSearchRequest() {
    SolrQuery query = new SolrQuery(queryStringBox.getText());
    query.setSort("index_date", ORDER.desc);
    query.addFacetField("sender");
    query.addFacetField("method");
    query.addFacetField("type");
    query.addFacetField("label");
    query.setFacetMinCount(1);
    return query;
  }
}
