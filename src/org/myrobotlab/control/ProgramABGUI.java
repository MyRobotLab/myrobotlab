package org.myrobotlab.control;

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
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.ProgramAB;
import org.myrobotlab.service.ProgramAB.Response;
import org.slf4j.Logger;

/**
 * A UI for controlling and interacting with ProgramD
 * 
 * @author kwatters
 *
 */
public class ProgramABGUI extends ServiceGUI implements ActionListener {

	public final static Logger log = LoggerFactory.getLogger(ProgramABGUI.class.toString());
	static final long serialVersionUID = 1L;
	public final String boundServiceName;
	static final String START_SESSION_LABEL = "Start Session";
	// TODO: make this auto-resize when added to gui..
	private JTextField text = new JTextField("By your command:", 60);
	private JTextArea response = new JTextArea("Program AB Response:");
	private JButton askButton = new JButton("Ask Program AB");
	private JScrollPane scrollResponse = new JScrollPane(response);

	private JTextField progABPath = new JTextField(new File("ProgramAB").getAbsolutePath(), 16);
	private JTextField botName = new JTextField("alice2", 16);

	private JButton startSessionButton = new JButton(START_SESSION_LABEL);
	private JButton saveAIML = new JButton("Save AIML");
	private JButton savePredicates = new JButton("Save Predicates");

	public ProgramABGUI(String boundServiceName, GUIService myService, JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		this.boundServiceName = boundServiceName;
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
			if (startSessionButton.getText().equals(START_SESSION_LABEL)) {
				myService.send(boundServiceName, "startSession", progABPath.getText().trim(), botName.getText().trim());
				startSessionButton.setText("Reload Session");
			} else {
				myService.send(boundServiceName, "reloadSession", progABPath.getText().trim(), botName.getText().trim());
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
	public void attachGUI() {
		//
		subscribe("publishState", "getState", ProgramAB.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		//
		unsubscribe("publishState", "getState", ProgramAB.class);
	}

	public void getState(final ProgramAB programab) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

			}
		});

	}

	@Override
	public void init() {
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

}
