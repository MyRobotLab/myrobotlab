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
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.Console;
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
  // TODO: make this auto-resize when added to gui..
  private JTextField text = new JTextField("", 30);
  private JEditorPane response = new JEditorPane("text/html", "");

  final Console debugJavaConsole = new Console();

  HTMLDocument responseDoc = new HTMLDocument();
  HTMLEditorKit responseKit = new HTMLEditorKit();
  StyleSheet cssKit = responseDoc.getStyleSheet();

  JLabel askLabel = new JLabel();

  private JButton askButton = new JButton("Send it");
  private JScrollPane scrollResponse = new JScrollPane(response);
  private JTextField progABPath = new JTextField(new File("ProgramAB").getAbsolutePath(), 16);
  private JTextField userName = new JTextField();
  private JComboBox<String> botName = new JComboBox<String>();
  private JButton startSession = new JButton("New session");
  private JButton reloadSession = new JButton("Reload chatbot");
  private JButton saveAIML = new JButton("Save AIML");
  private JButton savePredicates = new JButton("Save predicates");
  private String[] logsClassOnly = { "org.alicebot.ab.Graphmaster", "org.alicebot.ab.MagicBooleans", "class org.myrobotlab.programab.MrlSraixHandler" };

  JCheckBox filter = new JCheckBox("Filter ( ' , - )");
  JCheckBox visualDebug = new JCheckBox("Debug : ");

  JLabel pathP = new JLabel();
  ImageIcon pathI = Util.getImageIcon("FileConnector.png");
  JLabel userP = new JLabel();
  ImageIcon userI = Util.getImageIcon("user.png");
  JLabel botnameP = new JLabel();
  ImageIcon botnameI = Util.getImageIcon("chatbot.png");
  
  JLabel currentBotPath = new JLabel("");

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

    scrollResponse.setAutoscrolls(true);

    display.setLayout(new BorderLayout());
    debugJavaConsole.getTextArea().setBackground(Color.BLACK);
    debugJavaConsole.getTextArea().setForeground(Color.WHITE);
    Font font = debugJavaConsole.getTextArea().getFont();
    float size = font.getSize() - 1.0f;
    debugJavaConsole.getTextArea().setFont(font.deriveFont(size));
    debugJavaConsole.getScrollPane().setAutoscrolls(true);
    debugJavaConsole.getScrollPane().setPreferredSize(new Dimension(150, 100));

    JPanel northPanel = new JPanel(new GridLayout(2, 1));

    JPanel userSub = new JPanel();
    userSub.add(userP);
    userSub.add(userName);
    userSub.add(startSession);

    JPanel inputControlSub = new JPanel();
    inputControlSub.add(askLabel);
    inputControlSub.add(text);
    inputControlSub.add(askButton);
    inputControlSub.add(filter);
    northPanel.add(userSub);
    northPanel.add(inputControlSub);

    display.add(northPanel, BorderLayout.NORTH);

    display.add(scrollResponse, BorderLayout.CENTER);

    JPanel PAGEENDLeft = new JPanel(new GridLayout(2, 1));
    JPanel PAGEEND = new JPanel(new GridLayout(1, 2));

    JPanel botControl = new JPanel(new GridLayout(0, 2));
    JPanel buttons = new JPanel();

    botControl.add(pathP);
    botControl.add(progABPath);
    botControl.add(botnameP);
    botControl.add(botName);
    JLabel cbp = new JLabel("Current Bot Path");
    cbp.setIcon(Util.getImageIcon("FileConnector.png"));
    botControl.add(cbp);
    botControl.add(currentBotPath);
    
    buttons.add(saveAIML);
    buttons.add(reloadSession);
    buttons.add(savePredicates);
    buttons.add(visualDebug);

    PAGEENDLeft.add(botControl);
    PAGEENDLeft.add(buttons);

    PAGEEND.add(PAGEENDLeft);
    PAGEEND.add(debugJavaConsole.getScrollPane(), BorderLayout.CENTER);

    // display.add(botControl, BorderLayout.SOUTH);
    display.add(PAGEEND, BorderLayout.PAGE_END);

    text.addActionListener(this);
    askButton.addActionListener(this);
    startSession.addActionListener(this);
    reloadSession.addActionListener(this);
    saveAIML.addActionListener(this);
    savePredicates.addActionListener(this);
    visualDebug.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    String path = progABPath.getText().trim();
    String user = userName.getText().trim();
    String bot = null;
    if (botName.getItemCount() > 0) {
      bot = botName.getSelectedItem().toString();
    }
    if (o == askButton || o == text) {
      String textFiltered = text.getText();
      if (filter.isSelected()) {
        textFiltered = textFiltered.replace("'", " ").replace("-", " ");
      }
      // response.setText(response.getText() + "<br/>\n\r" + answer);
      send("getResponse", textFiltered);

      // clear out the original question.
      text.setText("");

    } else if (o == startSession) {
      swingGui.send(boundServiceName, "setPath", path);
      // TODO remove the last parameter, after CSV dead
      swingGui.send(boundServiceName, "startSession", path, user, bot);
    } else if (o == reloadSession) {
      swingGui.send(boundServiceName, "reloadSession", user, bot);
    } else if (o == saveAIML) {
      swingGui.send(boundServiceName, "writeAIML");
    } else if (o == visualDebug) {
      swingGui.send(boundServiceName, "setVisualDebug", visualDebug.isSelected());
    } else if (o == savePredicates) {
      swingGui.send(boundServiceName, "savePredicates");
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

  public void enableVisualDebug() {
    if (!debugJavaConsole.isStarted()) {
      debugJavaConsole.append("AIML debug ON :");
      // force info log for specific class to feed debug window
      debugJavaConsole.startLogging(logsClassOnly);
    }
  }

  public void disableVisualDebug() {
    if (debugJavaConsole.isStarted()) {
      debugJavaConsole.append("AIML debug OFF :");
      debugJavaConsole.stopLogging();
    }
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishText");
    unsubscribe("publishRequest");
    unsubscribe("publishOOBText");
    disableVisualDebug();
  }

  public void onText(String text) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          responseKit.insertHTML(responseDoc, responseDoc.getLength(), "<i><b>" + botName.getSelectedItem().toString() + "</b>: " + text.replace("  ", " ").trim() + "</i>", 0, 0,
              null);
        } catch (Exception e) {
          log.error("ProgramAB onText error : {}", e);
        }
        response.setCaretPosition(response.getDocument().getLength());
      }
    });
  }

  public void onRequest(String text) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        setLogLevelForConsole();
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
        debugJavaConsole.append("[OOB] " + text.replaceAll("\\s+", "").trim());
      }
    });
  }

  private void setLogLevelForConsole() {
    if (LoggingFactory.getInstance().getLevel() == "WARN" || LoggingFactory.getInstance().getLevel() == "ERROR") {
      for (String s : logsClassOnly) {
        if (!LoggerFactory.getLogger(s).isInfoEnabled()) {
          LoggingFactory.getInstance().setLevel(s, "INFO");
        }
      }
    }
  }

  public void onState(final ProgramAB programab) {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        String botname = programab.getCurrentBotName();
        String username = programab.getCurrentUserName();
        startSession.setEnabled(true);
        if (programab.getSessions().isEmpty() || !programab.isReady()) {
          startSession.setBackground(Color.ORANGE);
        } else {
          startSession.setBackground(Color.GREEN);
        }
        progABPath.setText(new File(programab.getPath()).getAbsolutePath());
        currentBotPath.setText(new File(programab.getPath()).getAbsolutePath() + File.separator + "bots" + File.separator + botname);
        userName.setText(username);
        botName.removeAllItems();
        Iterator<String> iterator = programab.getBots().iterator();

        while (iterator.hasNext()) {
          botName.addItem(iterator.next());
        }
        if (programab.getCurrentBotName() != null) {
          botName.setSelectedItem(programab.getCurrentBotName());
        }
        visualDebug.setSelected(programab.getVisualDebug());
        if (programab.getVisualDebug()) {
          enableVisualDebug();
        } else {
          disableVisualDebug();
        }
      }
    });

  }

}