package org.myrobotlab.ui.autocomplete;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

/**
 * A simple example showing how to do search and replace in a RSyntaxTextArea.
 * The toolbar isn't very user-friendly, but this is just to show you how to use
 * the API.
 * <p>
 * 
 * This example uses RSyntaxTextArea 2.0.1.
 * <p>
 * 
 * Project Home: http://fifesoft.com/rsyntaxtextarea<br>
 * Downloads: https://sourceforge.net/projects/rsyntaxtextarea
 */
public class FindAndReplaceDemo extends JFrame implements ActionListener {

  private static final long serialVersionUID = 1L;

  private RSyntaxTextArea textArea;
  private JTextField searchField;
  private JCheckBox regexCB;
  private JCheckBox matchCaseCB;

  public static void main(String[] args) {
    // Start all SwingGui applications on the EDT.
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          String laf = UIManager.getSystemLookAndFeelClassName();
          UIManager.setLookAndFeel(laf);
        } catch (Exception e) { /* never happens */
        }
        FindAndReplaceDemo demo = new FindAndReplaceDemo();
        demo.setVisible(true);
        demo.textArea.requestFocusInWindow();
      }
    });
  }

  public FindAndReplaceDemo() {

    JPanel cp = new JPanel(new BorderLayout());

    textArea = new RSyntaxTextArea(20, 60);
    textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
    textArea.setCodeFoldingEnabled(true);
    textArea.setAntiAliasingEnabled(true);
    RTextScrollPane sp = new RTextScrollPane(textArea);
    sp.setFoldIndicatorEnabled(true);
    cp.add(sp);

    // Create a toolbar with searching options.
    JToolBar toolBar = new JToolBar();
    searchField = new JTextField(30);
    toolBar.add(searchField);
    final JButton nextButton = new JButton("Find Next");
    nextButton.setActionCommand("FindNext");
    nextButton.addActionListener(this);
    toolBar.add(nextButton);
    searchField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        nextButton.doClick(0);
      }
    });
    JButton prevButton = new JButton("Find Previous");
    prevButton.setActionCommand("FindPrev");
    prevButton.addActionListener(this);
    toolBar.add(prevButton);
    regexCB = new JCheckBox("Regex");
    toolBar.add(regexCB);
    matchCaseCB = new JCheckBox("Match Case");
    toolBar.add(matchCaseCB);
    cp.add(toolBar, BorderLayout.NORTH);

    setContentPane(cp);
    setTitle("Find and Replace Demo");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    pack();
    setLocationRelativeTo(null);

  }

  @Override
  public void actionPerformed(ActionEvent e) {

    // "FindNext" => search forward, "FindPrev" => search backward
    String command = e.getActionCommand();
    boolean forward = "FindNext".equals(command);

    // Create an object defining our search parameters.
    SearchContext context = new SearchContext();
    String text = searchField.getText();
    if (text.length() == 0) {
      return;
    }
    context.setSearchFor(text);
    context.setMatchCase(matchCaseCB.isSelected());
    context.setRegularExpression(regexCB.isSelected());
    context.setSearchForward(forward);
    context.setWholeWord(false);

    boolean found = SearchEngine.find(textArea, context);
    if (!found) {
      JOptionPane.showMessageDialog(this, "Text not found");
    }

  }

}
