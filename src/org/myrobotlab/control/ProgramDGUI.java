package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.ProgramD;
import org.myrobotlab.service.Servo;
import org.slf4j.Logger;

/**
 * A UI for controlling and interacting with ProgramD
 * 
 * @author kwatters
 *
 */
public class ProgramDGUI extends ServiceGUI implements ActionListener {

	public final static Logger log = LoggerFactory.getLogger(ProgramDGUI.class.toString());
	static final long serialVersionUID = 1L;
	public final String boundServiceName;
	
	// TODO: make this auto-resize when added to gui..
	private JTextField text = new JTextField("Say Hello:", 60);
	private JTextArea response = new JTextArea("Program D Response:");
	private JButton askButton = new JButton("Ask Program D");
	private JScrollPane scrollResponse = new JScrollPane(response);
	
	private JTextField userId = new JTextField("User1", 16);
	private JTextField botName = new JTextField("SampleBot", 16);
	
	private JTextField programDPath = new JTextField("C:\\tools\\ProgramD\\");
	private JTextField programDCorePath = new JTextField("C:\\tools\\ProgramD\\conf\\core.xml");
	private JButton loadCoreButton = new JButton("Load Core");
	
	public ProgramDGUI(String boundServiceName, GUIService myService,JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		this.boundServiceName = boundServiceName;
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
		JLabel userLabel = new JLabel("User ID:");
		botControl.add(userLabel);
		botControl.add(userId);
		JLabel botLabel = new JLabel("Bot ID:");
		botControl.add(botLabel);
		botControl.add(botName);
		
		botControl.add(programDPath);
		botControl.add(programDCorePath);
		botControl.add(loadCoreButton);
		
		display.add(botControl, BorderLayout.PAGE_END);
		
		text.addActionListener(this);
		askButton.addActionListener(this);
		
		loadCoreButton.addActionListener(this);
		
		
	}

	@Override
	public void attachGUI() {
		// 
		subscribe("publishState", "getState", ProgramD.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		// 
		unsubscribe("publishState", "getState", ProgramD.class);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == askButton || o == text) {
			//myService.send(boundServiceName, "getResponse", new String(text.getText()), "1", "SampleBot");
			String answer=(String) myService.sendBlocking(boundServiceName, "getResponse", text.getText(), userId.getText().trim(), botName.getText().trim());
			// response.setText(response.getText() + "<br/>\n\r" + answer);
			if (answer != null) {
				response.append("\n" + answer.trim());
			} else {
				response.append("\nERROR: NULL Response");
			}
			// clear out the original question.
			text.setText("");			
		} if (o == loadCoreButton) {
			myService.send(boundServiceName, "loadCore", programDPath.getText().trim(), programDCorePath.getText().trim());
		} else {
			log.info("Unknown action!");
		}
		// TODO Auto-generated method stub
	}
	
}
