package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.ProgramAB.Response;
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
  private JTextField text = new JTextField("By your command:", 60);
  private JTextArea response = new JTextArea("Program AB Response:");
  private JButton askButton = new JButton("Ask Program AB");
  private JScrollPane scrollResponse = new JScrollPane(response);

  private JTextField progABPath = new JTextField(new File("ProgramAB").getAbsolutePath(), 16);
  private JTextField userName = new JTextField("default", 16);
  private JTextField botName = new JTextField("alice2", 16);

  private JButton startSessionButton = new JButton(START_SESSION_LABEL);
  private JButton saveAIML = new JButton("Save AIML");
  private JButton savePredicates = new JButton("Save Predicates");

  public ProgramABGui(String boundServiceName, SwingGui myService, JTabbedPane tabs) {
    super(boundServiceName, myService, tabs);
    this.boundServiceName = boundServiceName;

    //
    scrollResponse.setAutoscrolls(true);
    display.setLayout(new BorderLayout());

    JPanel inputControl = new JPanel();

    inputControl.add(text);
    inputControl.add(askButton);

    display.add(inputControl, BorderLayout.PAGE_START);

    display.add(scrollResponse, BorderLayout.CENTER);

    JPanel botControl = new JPanel();

    botControl.add(progABPath);
    botControl.add(userName);
    botControl.add(botName);
    botControl.add(startSessionButton);
    botControl.add(saveAIML);
    botControl.add(savePredicates);

    display.add(botControl, BorderLayout.PAGE_END);

    text.addActionListener(this);
    askButton.addActionListener(this);

    startSessionButton.addActionListener(this);

    saveAIML.addActionListener(this);
    savePredicates.addActionListener(this);

  
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    if (o == askButton || o == text) {
      // myService.send(boundServiceName, "getResponse", new
      // String(text.getText()), "1", "SampleBot");

      Response answer = (Response) myService.sendBlocking(boundServiceName, 10000, "getResponse", text.getText());
      // response.setText(response.getText() + "<br/>\n\r" + answer);
      if (answer != null) {
        response.append("\n" + answer.msg.trim());
      } else {
        response.append("\nERROR: NULL Response");
      }
      // clear out the original question.
      text.setText("");
    } else if (o == startSessionButton) {
      String path = progABPath.getText().trim();
      String user = userName.getText().trim();
      String bot = botName.getText().trim();
      if (startSessionButton.getText().equals(START_SESSION_LABEL)) {
        myService.send(boundServiceName, "startSession", path, user, bot);
        startSessionButton.setText("Reload Session");
      } else {
        myService.send(boundServiceName, "reloadSession", path, user, bot);
      }
    } else if (o == saveAIML) {
      myService.send(boundServiceName, "writeAIML");
      myService.send(boundServiceName, "writeAIMLIF");
    } else if (o == savePredicates) {
      myService.send(boundServiceName, "savePredicates");
    } else {
      log.info(o.toString());
      log.info("Unknown action!");
    }
    // TODO Auto-generated method stub
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

      }
    });

  }


}
