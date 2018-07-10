package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class SolrGui extends ServiceGui implements ActionListener {

  private String boundServiceName;
  private JTextField queryStringBox = new JTextField("*:*", 30);
  private JTextArea results = new JTextArea("Search Results:");
  private JScrollPane scrollResponse = new JScrollPane(results);
  private JButton searchButton = new JButton("Search");
  
  public SolrGui(String boundServiceName, SwingGui myService) {
    super(boundServiceName, myService);
    // search box.
    this.boundServiceName = boundServiceName;
    queryStringBox.setFont(new Font("Arial", Font.BOLD, 14));
    queryStringBox.setPreferredSize(new Dimension(40, 35));
    //
    scrollResponse.setAutoscrolls(true);
    display.setLayout(new BorderLayout());

    JPanel inputControlSub = new JPanel();
    inputControlSub.add(queryStringBox);
    inputControlSub.add(searchButton);
    
    display.add(inputControlSub, BorderLayout.NORTH);
    display.add(scrollResponse, BorderLayout.CENTER);
    // TODO: build a search result area

    searchButton.addActionListener(this);
    queryStringBox.addActionListener(this);
    
    
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
    SolrQuery query = new SolrQuery(queryStringBox.getText());
    query.setSort("index_date", ORDER.desc);
    QueryResponse answer = (QueryResponse) swingGui.sendBlocking(boundServiceName, 10000, "search", query);
    // TODO: build up a search result page.
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
}
