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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
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
  static final String START_SESSION_LABEL = "Start Session";
  // TODO: make this auto-resize when added to gui..
  private JTextField text = new JTextField("", 30);
  private JEditorPane response = new JEditorPane("text/html", "");
  HTMLDocument responseDoc = new HTMLDocument();
  HTMLEditorKit responseKit = new HTMLEditorKit();

  JLabel askLabel = new JLabel();

  private JButton askButton = new JButton("Send it");
  private JScrollPane scrollResponse = new JScrollPane(response);
  private JTextField progABPath = new JTextField(new File("ProgramAB").getAbsolutePath(), 16);
  private JTextField userName = new JTextField();
  private JTextField botName = new JTextField();
  private JButton startSessionButton = new JButton(START_SESSION_LABEL);
  JButton reloadSession = new JButton("New session");
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
    userP.setText("User name : ");
    botnameP.setIcon(botnameI);
    botnameP.setText("Bot subfolderName : ");
    text.setFont(new Font("Arial", Font.BOLD, 14));
    text.setPreferredSize(new Dimension(40, 35));
    askLabel.setText("Ask : ");
    filter.setSelected(true);

    response.setAutoscrolls(true);
    response.setEditable(false);
    response.setEditorKit(responseKit);
    response.setDocument(responseDoc);

    responseKit.insertHTML(responseDoc, responseDoc.getLength(), "<font face=\\\"Verdana\\\">Conversation :<br/>", 0, 0, null);

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
    botControl.add(startSessionButton);

    botControl.add(userP);
    botControl.add(userName);
    botControl.add(reloadSession);

    botControl.add(botnameP);
    botControl.add(botName);
    botControl.add(saveAIML);

    PAGEEND.add(botControl);

    // display.add(botControl, BorderLayout.SOUTH);
    display.add(PAGEEND, BorderLayout.PAGE_END);

    text.addActionListener(this);
    askButton.addActionListener(this);
    startSessionButton.addActionListener(this);
    reloadSession.addActionListener(this);
    saveAIML.addActionListener(this);

  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
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
    } else if (o == startSessionButton) {
      String path = progABPath.getText().trim();
      String user = userName.getText().trim();
      String bot = botName.getText().trim();
      swingGui.send(boundServiceName, "setPath", path);
      swingGui.send(boundServiceName, "reloadSession", path, user, bot, true);
    } else if (o == reloadSession) {
      swingGui.send(boundServiceName, "startSession", userName.getText().trim());
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
          responseKit.insertHTML(responseDoc, responseDoc.getLength(), "<i><b><br>" + botName.getText() + "</b>: " + text.replaceAll("\\<.*?\\>", " ").trim(), 0, 0, Tag.I);
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
          responseKit.insertHTML(responseDoc, responseDoc.getLength(), "<font color=blue><b><br> > OOB: </b>" + filteredOOB, 0, 0, Tag.FONT);
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
        startSessionButton.setEnabled(true);
        if (programab.getSessions().isEmpty()) {
          startSessionButton.setText("Start Session");
          startSessionButton.setBackground(Color.RED);
        } else {
          startSessionButton.setText("Reload Chatbot");
          startSessionButton.setBackground(Color.GREEN);
        }
        if (programab.loading) {
          startSessionButton.setText("Loading...");
          startSessionButton.setBackground(Color.ORANGE);
          startSessionButton.setEnabled(false);
        }
        if (programab.aimlError) {
          startSessionButton.setText("AIML error");
          startSessionButton.setBackground(Color.RED);
          startSessionButton.setEnabled(false);
        }
        progABPath.setText(new File(programab.getPath()).getAbsolutePath());
        userName.setText(username);
        botName.setText(botname);

      }
    });

  }

}