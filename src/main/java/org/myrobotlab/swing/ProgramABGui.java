package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.ProgramAB.Response;
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
  private JTextArea response = new JTextArea("BOT Response :");
  JLabel askLabel = new JLabel();
  JLabel nothingLabel = new JLabel();

  private JButton askButton = new JButton("Send it");
  private JScrollPane scrollResponse = new JScrollPane(response);

  private JTextField progABPath = new JTextField(new File("ProgramAB").getAbsolutePath(), 16);
  private JTextField userName = new JTextField("default", 16);
  private JTextField botName = new JTextField("alice2", 16);

  private JButton startSessionButton = new JButton(START_SESSION_LABEL);
  private JButton killAiml = new JButton("Restart (w/o AIMLiF)");
  private JButton reloadSession = new JButton("Reload session");
  private JButton saveAIML = new JButton("Save AIML");
  private JButton savePredicates = new JButton("Save Predicates");

  JCheckBox filter = new JCheckBox("Filter ( ' , - )");

  JLabel pathP = new JLabel();
  ImageIcon pathI = Util.getImageIcon("GPS.png");
  JLabel userP = new JLabel();
  ImageIcon userI = Util.getImageIcon("FindHuman.png");
  JLabel botnameP = new JLabel();
  ImageIcon botnameI = Util.getImageIcon("robot.png");

  public ProgramABGui(String boundServiceName, SwingGui myService) {
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

    JPanel botControl = new JPanel(new GridLayout(3, 4));

    botControl.add(pathP);
    botControl.add(progABPath);
    botControl.add(startSessionButton);
    botControl.add(killAiml);

    botControl.add(userP);
    botControl.add(userName);
    botControl.add(reloadSession);
    botControl.add(savePredicates);

    botControl.add(botnameP);
    botControl.add(botName);
    botControl.add(saveAIML);
    botControl.add(nothingLabel);

    PAGEEND.add(botControl);

    // display.add(botControl, BorderLayout.SOUTH);
    display.add(PAGEEND, BorderLayout.PAGE_END);

    text.addActionListener(this);
    askButton.addActionListener(this);

    startSessionButton.addActionListener(this);
    killAiml.addActionListener(this);
    reloadSession.addActionListener(this);

    saveAIML.addActionListener(this);
    savePredicates.addActionListener(this);

  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    if (o == askButton || o == text) {
      String textFiltered = text.getText();
      if (filter.isSelected()) {
        textFiltered = textFiltered.replace("'", " ").replace("-", " ");
      }
      Response answer = (Response) swingGui.sendBlocking(boundServiceName, 10000, "getResponse", textFiltered);
      // response.setText(response.getText() + "<br/>\n\r" + answer);
      if (answer != null) {
        response.append("\n" + answer.msg.trim());
      } else {
        response.append("\nERROR: NULL Response");
      }
      // clear out the original question.
      text.setText("");
      response.setCaretPosition(response.getDocument().getLength());
    } else if (o == startSessionButton) {
      String path = progABPath.getText().trim();
      String user = userName.getText().trim();
      String bot = botName.getText().trim();
      swingGui.send(boundServiceName, "setPath", path);
      swingGui.send(boundServiceName, "reloadSession", path, user, bot);

    } else if (o == killAiml) {
      String path = progABPath.getText().trim();
      String user = userName.getText().trim();
      String bot = botName.getText().trim();
      swingGui.send(boundServiceName, "setPath", path);
      swingGui.send(boundServiceName, "reloadSession", path, user, bot, true);
    } else if (o == reloadSession) {
      swingGui.send(boundServiceName, "setUsername", userName.getText().trim());
    } else if (o == saveAIML) {
      swingGui.send(boundServiceName, "writeAIML");
      swingGui.send(boundServiceName, "writeAIMLIF");
    } else if (o == savePredicates) {
      swingGui.send(boundServiceName, "savePredicates");
    } else {
      log.info(o.toString());
      log.info("Unknown action!");
    }
  }

  @Override
  public void subscribeGui() {
  }

  @Override
  public void unsubscribeGui() {
  }

  public void onState(final ProgramAB programab) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        startSessionButton.setEnabled(true);
        if (programab.getSessions().isEmpty()) {
          startSessionButton.setText("Start Session");
          startSessionButton.setBackground(Color.RED);
        } else {
          startSessionButton.setText("Restart Chatbot");
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
        userName.setText(programab.getCurrentUserName());
        botName.setText(programab.getCurrentBotName());
      }
    });

  }

}
