package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.HtmlFilter;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

/**
 * A UI for controlling and interacting with ProgramD
 * 
 * @author kwatters
 *
 */
public class ProgramABGui extends ServiceGui implements ActionListener {

  public final static Logger log = LoggerFactory.getLogger(ProgramABGui.class.toString());
  static final long serialVersionUID = 1L;
  public final String boundServiceName;
  static final String START_SESSION_LABEL = "Start chatbot";
  // TODO: make this auto-resize when added to gui..
  private JTextField text = new JTextField("", 30);
  private JEditorPane response = new JEditorPane("text/html", "");
  HTMLDocument responseDoc = new HTMLDocument();
  HTMLEditorKit responseKit = new HTMLEditorKit();
  StyleSheet cssKit = responseDoc.getStyleSheet();

  JLabel askLabel = new JLabel();

  private JButton askButton = new JButton("Send it");
  private JScrollPane scrollResponse = new JScrollPane(response);
  private JTextField progABPath = new JTextField(new File("ProgramAB").getAbsolutePath(), 16);
  private JTextField userName = new JTextField();
  private JComboBox<String> botName = new JComboBox<String>();
  private JButton startChatbotButton = new JButton(START_SESSION_LABEL);
  JButton newSession = new JButton("New session");
  private JButton saveAIML = new JButton("Save AIML");

  JCheckBox filter = new JCheckBox("Filter ( ' , - )");

  JLabel pathP = new JLabel();
  ImageIcon pathI = Util.getImageIcon("GPS.png");
  JLabel userP = new JLabel();
  ImageIcon userI = Util.getImageIcon("FindHuman.png");
  JLabel botnameP = new JLabel();
  ImageIcon botnameI = Util.getImageIcon("robot.png");

  public ProgramABGui(String boundServiceName, SwingGui myService) throws BadLocationException, IOException {
    super(boundServiceName, myService);
    this.boundServiceName = boundServiceName;
    pathP.setText("Path : ");
    pathP.setIcon(pathI);
    userP.setIcon(userI);
    userP.setText("User : ");
    botnameP.setIcon(botnameI);
    botnameP.setText("Bot : ");
    text.setFont(new Font("Arial", Font.BOLD, 14));
    text.setPreferredSize(new Dimension(40, 35));
    askLabel.setText("Ask : ");
    filter.setSelected(true);
    cssKit.addRule("body {font-family:\"Verdana\";}");
    response.setAutoscrolls(true);
    response.setEditable(false);
    response.setEditorKit(responseKit);
    response.setDocument(responseDoc);

    responseKit.insertHTML(responseDoc, responseDoc.getLength(), "Conversation :<br/>", 0, 0, null);

    //
    scrollResponse.setAutoscrolls(true);
    display.setLayout(new BorderLayout());

    JPanel inputControlSub = new JPanel();
    inputControlSub.add(askLabel);
    inputControlSub.add(text);
    inputControlSub.add(askButton);
    inputControlSub.add(filter);

    display.add(inputControlSub, BorderLayout.NORTH);

    display.add(scrollResponse, BorderLayout.CENTER);

    JPanel PAGEEND = new JPanel();

    JPanel botControl = new JPanel(new GridLayout(3, 3));

    botControl.add(pathP);
    botControl.add(progABPath);
    botControl.add(startChatbotButton);

    botControl.add(userP);
    botControl.add(userName);
    botControl.add(newSession);

    botControl.add(botnameP);
    botControl.add(botName);
    botControl.add(saveAIML);

    PAGEEND.add(botControl);

    // display.add(botControl, BorderLayout.SOUTH);
    display.add(PAGEEND, BorderLayout.PAGE_END);

    text.addActionListener(this);
    askButton.addActionListener(this);
    startChatbotButton.addActionListener(this);
    newSession.addActionListener(this);
    saveAIML.addActionListener(this);

  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    String path = progABPath.getText().trim();
    String user = userName.getText().trim();
    String bot = botName.getSelectedItem().toString();
    if (o == askButton || o == text) {
      String textFiltered = text.getText();
      if (filter.isSelected()) {
        textFiltered = textFiltered.replace("'", " ").replace("-", " ");
      }
      // response.setText(response.getText() + "<br/>\n\r" + answer);
      send("getResponse", textFiltered);

      // clear out the original question.
      text.setText("");
      response.setCaretPosition(response.getDocument().getLength());

    } else if (o == startChatbotButton) {
      swingGui.send(boundServiceName, "setPath", path);
      // TODO remove the last parameter, after CSV dead
      swingGui.send(boundServiceName, "reloadSession", path, user, bot, true);
    } else if (o == newSession) {
      swingGui.send(boundServiceName, "startSession", user, bot);
    } else if (o == saveAIML) {
      swingGui.send(boundServiceName, "writeAIML");
      swingGui.send(boundServiceName, "writeAIMLIF");
    } else {
      log.info(o.toString());
      log.info("Unknown action!");
    }
  }

  @Override
  public void subscribeGui() {
    subscribe("publishRequest");
    subscribe("publishText");
    subscribe("publishOOBText");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishText");
    unsubscribe("publishRequest");
    unsubscribe("publishOOBText");
  }

  public void onText(String text) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          responseKit.insertHTML(responseDoc, responseDoc.getLength(), "<i><b>" + botName.getSelectedItem().toString() + "</b>: " + HtmlFilter.stripHtml(text).trim() + "</i>", 0,
              0, null);
        } catch (Exception e) {
          log.error("ProgramAB onText error : {}", e);
        }
      }
    });
  }

  public void onRequest(String text) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          responseKit.insertHTML(responseDoc, responseDoc.getLength(), "<b>" + userName.getText() + "</b>: " + text, 0, 0, null);
        } catch (Exception e) {
          log.error("ProgramAB onRequest error : {}", e);
        }
      }
    });
  }

  public void onOOBText(String text) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          String filteredOOB = "";
          Pattern oobPattern = Pattern.compile("<param>(.+?)</param>");
          Matcher oobMatcher = oobPattern.matcher(text);
          filteredOOB = oobMatcher.find() ? filteredOOB = oobMatcher.group(1) : "";
          responseKit.insertHTML(responseDoc, responseDoc.getLength(), "<font color=blue><b>&nbsp;&gt;&nbsp;OOB: </b>" + filteredOOB + "</font>", 0, 0, null);
        } catch (BadLocationException | IOException e) {
          log.error("ProgramAB onOOBText error : {}", e);
        }
      }
    });
  }

  public void onState(final ProgramAB programab) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        String botname = programab.getCurrentBotName();
        String username = programab.getCurrentUserName();
        startChatbotButton.setEnabled(true);
        if (programab.getSessions().isEmpty() || programab.aimlError) {
          startChatbotButton.setText("Start Session");
          startChatbotButton.setBackground(Color.RED);
        } else {
          startChatbotButton.setText("Reload Chatbot");
          startChatbotButton.setBackground(Color.GREEN);
        }
        if (programab.loading) {
          startChatbotButton.setText("Loading...");
          startChatbotButton.setBackground(Color.ORANGE);
          startChatbotButton.setEnabled(false);
        }
        progABPath.setText(new File(programab.getPath()).getAbsolutePath());
        userName.setText(username);

        botName.removeAllItems();
        Iterator<String> iterator = programab.getBots().iterator();

        while (iterator.hasNext()) {
          botName.addItem(iterator.next());
        }
        if (programab.getCurrentBotName() != null) {
          botName.setSelectedItem(programab.getCurrentBotName());
        }

      }
    });

  }

}