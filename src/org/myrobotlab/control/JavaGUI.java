/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultCaret;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.control.widget.Console;
import org.myrobotlab.control.widget.FileUtil;
import org.myrobotlab.control.widget.ImageButton;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.Python.Script;
import org.myrobotlab.ui.autocomplete.MRLCompletionProvider;

/**
 * Python GUIService
 * 
 * @author SwedaKonsult
 * 
 *         use - http://famfamfam.com/lab/icons/silk/previews/index_abc.png -
 *         SILK ICONS
 */
public class JavaGUI extends ServiceGUI implements ActionListener, MouseListener {

	// FIXME - should be part of separate "Editor" class
	static public class EditorPanel {
		String filename;
		TextEditorPane editor;// = new TextEditorPane(); <-- cant be null
								// constructor
		JScrollPane panel;// = createEditorPane();

		public EditorPanel(Script script) {
			try {
				filename = script.getName();
				editor = new TextEditorPane(RTextArea.INSERT_MODE, false, FileLocation.create(new File(filename)));
				editor.setText(script.getCode());
				editor.setCaretPosition(0);

				panel = createEditorPane();
			} catch (Exception e) {
				Logging.logError(e);
			}
		}

		private JScrollPane createEditorPane() {
			// editor tweaks
			editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			editor.setCodeFoldingEnabled(true);
			editor.setAntiAliasingEnabled(true);

			// auto-completion
			if (ac != null) {
				ac.install(editor);
				ac.setShowDescWindow(true);
			}

			return new RTextScrollPane(editor);
		}

		public String getDisplayName() {
			if (filename.startsWith("Java/examples/")) {

				return filename.substring("Java/examples/".length());

			} else {
				int begin = filename.lastIndexOf(File.separator);
				if (begin > 0) {
					++begin;
				} else {
					begin = 0;
				}

				return filename.substring(begin);
			}
		}

		public TextEditorPane getEditor() {
			return editor;
		}

		public String getFilename() {
			return filename;
		}
	}

	static final long serialVersionUID = 1L;
	private final static int fileMenuMnemonic = KeyEvent.VK_F;
	private static final int saveMenuMnemonic = KeyEvent.VK_S;
	private static final int openMenuMnemonic = KeyEvent.VK_O;

	private static final int examplesMenuMnemonic = KeyEvent.VK_X;

	final JFrame top;

	final JTabbedPane editorTabs;

	JSplitPane splitPane;

	final JLabel statusInfo;

	HashMap<String, EditorPanel> scripts = new HashMap<String, EditorPanel>();

	// TODO - check for outside modification with lastmoddate
	String currentScriptName;
	// button bar buttons
	ImageButton executeButton;

	ImageButton stopButton;
	ImageButton openFileButton;

	ImageButton saveFileButton;
	// consoles
	JTabbedPane consoleTabs;
	final Console javaConsole;
	final JTextArea pythonConsole;

	final JScrollPane pythonScrollPane;
	// auto-completion
	static CompletionProvider provider;

	static AutoCompletion ac;

	int untitledCount = 1;

	/**
	 * Constructor
	 * 
	 * @param boundServiceName
	 * @param myService
	 */
	public JavaGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);

		javaConsole = new Console();
		pythonConsole = new JTextArea();
		pythonScrollPane = new JScrollPane(pythonConsole);

		// autocompletion - in the constructor so that they can be declared
		// final
		// provider = createCompletionProvider(); FIXME - takes forever
		// ac = new AutoCompletion(provider);

		provider = null;
		ac = null;

		currentScriptName = null;

		editorTabs = new JTabbedPane();

		splitPane = null;

		statusInfo = new JLabel("Status:");
		top = myService.getFrame();

		Script s = new Script(String.format("%s%suntitled.%d.java", Service.getCFGDir(), File.separator, untitledCount), "");
		addNewEditorPanel(s);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		Object o = arg0.getSource();
		if (o == stopButton) {
			performStop();
			return;
		} else if (o == executeButton) {
			performExecute();
			return;
		} else if (o == saveFileButton) {
			saveFile();
			return;
		} else if (o == openFileButton) {
			openFile();
			return;
		}

		if (!(o instanceof JMenuItem)) {
			return;
		}
		JMenuItem m = (JMenuItem) o;
		if (m.getText().equals("new")) {
			++untitledCount;
			Script s = new Script(String.format("%s%suntitled.%d.java", Service.getCFGDir(), File.separator, untitledCount), "");
			addNewEditorPanel(s);
		} else if (m.getText().equals("save")) {
			saveFile();
		} else if (m.getText().equals("open")) {
			openFile();
		} else if (m.getText().equals("save as")) {
			saveAsFile();
		} else if (m.getText().equals("close")) {
			closeFile();
		} else if (m.getActionCommand().equals("examples")) {
			String filename = String.format("Python/examples/%1$s", m.getText());
			Script script = new Script(filename, FileIO.resourceToString(filename));
			addNewEditorPanel(script);
		}
	}

	public EditorPanel addNewEditorPanel(Script script) {
		EditorPanel panel = new EditorPanel(script);
		editorTabs.addTab(panel.getDisplayName(), panel.panel);
		log.info(panel.getEditor().getFileFullPath());
		GUIService gui = myService;// FIXME - bad bad bad ...
		TabControl2 tc = new TabControl2(self, editorTabs, panel.panel, panel.getFilename());
		tc.addMouseListener(this);
		editorTabs.setTabComponentAt(editorTabs.getTabCount() - 1, tc);
		currentScriptName = script.getName();
		scripts.put(script.getName(), panel);
		return panel;
	}

	public void appendScript(String data) {
		EditorPanel p = scripts.get(currentScriptName);
		if (p != null) {
			p.editor.setText(String.format("%s\n%s", p.editor.getText(), data));
		} else {
			log.error("can't append Script to current");
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Python.class);
		subscribe("finishedExecutingScript");
		subscribe("publishStdOut", "getStdOut", String.class);
		subscribe("appendScript", "appendScript", String.class);
		subscribe("startRecording", "startRecording", String.class);
		myService.send(boundServiceName, "attachPythonConsole");
		// myService.send(boundServiceName, "broadcastState");
	}

	public void closeFile() {
		if (scripts.containsKey(currentScriptName)) {
			EditorPanel p = scripts.get(currentScriptName);
			if (p.editor.isDirty()) {
				saveAsFile();
			}

			p = scripts.get(currentScriptName);
			scripts.remove(p);
			editorTabs.remove(p.panel);

		} else {
			log.error(String.format("can't closeFile %s", currentScriptName));
		}
	}

	/**
	 * 
	 * @return
	 */
	private CompletionProvider createCompletionProvider() {
		// TODO -> LanguageSupportFactory.get().register(editor);

		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		return new MRLCompletionProvider();
	}

	/**
	 * Fill up the examples menu with submenu items.
	 * 
	 * @param examples
	 */
	private void createExamplesMenu(JMenu examples) {
		// FIXME - dynamically build based on resources
		// JMenu menu;
		// menu = new JMenu("Arduino");
		// menu.add(createMenuItem("arduinoInput.java", "examples"));
		// menu.add(createMenuItem("arduinoOutput.java", "examples"));
		// menu.add(createMenuItem("arduinoLoopback.java", "examples"));
		// examples.add(menu);
		//
		// menu = new JMenu("Python");
		// menu.add(createMenuItem("createAService.java", "examples"));
		// menu.add(createMenuItem("basicPython.java", "examples"));
		// menu.add(createMenuItem("panTilt.java", "examples"));
		// examples.add(menu);
		//
		// menu = new JMenu("Services");
		// menu.add(createMenuItem("createAService.java", "examples"));
		//
		// menu = new JMenu("Input");
		// menu.add(createMenuItem("inputTest.java", "examples"));
		// examples.add(menu);
		//
		// menu = new JMenu("Speech");
		// menu.add(createMenuItem("sayThings.java", "examples"));
		// menu.add(createMenuItem("talkBack.java", "examples"));
		// examples.add(menu);
		//
		// menu = new JMenu("Robots");
		// menu.add(createMenuItem("houston.java", "examples"));
		// menu.add(createMenuItem("inMoovHandRobot.java", "examples"));
		// menu.add(createMenuItem("inMoovTalkMovement.java", "examples"));
		// examples.add(menu);
		//
		// menu = new JMenu("Vision");
		// menu.add(createMenuItem("faceTracking.java", "examples"));
		// menu.add(createMenuItem("colorTracking.java", "examples"));
		// menu.add(createMenuItem("lkOpticalTrack.java", "examples"));
		// examples.add(menu);
	}

	/**
	 * Fill up the file menu with submenu items.
	 * 
	 * @param fileMenu
	 */
	private void createFileMenu(JMenu fileMenu) {
		fileMenu.add(createMenuItem("new"));
		fileMenu.add(createMenuItem("save", saveMenuMnemonic, "control S", null));
		fileMenu.add(createMenuItem("save as"));
		fileMenu.add(createMenuItem("open", openMenuMnemonic, "control O", null));
		fileMenu.add(createMenuItem("close"));
		fileMenu.addSeparator();
	}

	/**
	 * Build the main portion of the view.
	 * 
	 * @return
	 */
	private JSplitPane createMainPane() {
		JSplitPane pane = new JSplitPane();

		consoleTabs = createTabsPane();

		pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorTabs, consoleTabs);
		pane.setDividerLocation(450);

		return pane;
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @return
	 */
	private JMenuItem createMenuItem(String label) {
		return createMenuItem(label, -1, null, null);
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @param vKey
	 * @param accelerator
	 * @param actionCommand
	 * @return
	 */
	private JMenuItem createMenuItem(String label, int vKey, String accelerator, String actionCommand) {
		JMenuItem mi = null;
		if (vKey == -1) {
			mi = new JMenuItem(label);
		} else {
			mi = new JMenuItem(label, vKey);
		}

		if (actionCommand != null) {
			mi.setActionCommand(actionCommand);
		}

		if (accelerator != null) {
			KeyStroke ctrlCKeyStroke = KeyStroke.getKeyStroke(accelerator);
			mi.setAccelerator(ctrlCKeyStroke);
		}

		mi.addActionListener(this);
		return mi;
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @param actionCommand
	 * @return
	 */
	private JMenuItem createMenuItem(String label, String actionCommand) {
		return createMenuItem(label, -1, null, actionCommand);
	}

	/**
	 * Build the top menu panel.
	 * 
	 * @return
	 */
	private JPanel createMenuPanel() {
		JMenuBar menuBar = createTopMenuBar();
		JPanel buttonBar = createTopButtonBar();

		JPanel menuPanel = new JPanel(new BorderLayout());
		menuPanel.add(menuBar, BorderLayout.LINE_START);
		menuPanel.add(buttonBar);

		return menuPanel;
	}

	/**
	 * Build the tabs pane.
	 * 
	 * @return
	 */
	private JTabbedPane createTabsPane() {
		JTabbedPane pane = new JTabbedPane();
		pane.addTab("java", javaConsole.getScrollPane());
		GUIService gui = myService;// FIXME - bad bad bad ...

		pane.setTabComponentAt(pane.getTabCount() - 1, new TabControl2(self, pane, javaConsole.getScrollPane(), "java"));

		// pane.addTab("python", pythonScrollPane);
		// pane.setTabComponentAt(pane.getTabCount() - 1, new TabControl(gui,
		// pane, pythonScrollPane, boundServiceName, "python"));

		return pane;
	}

	/**
	 * Build up the top button menu bar.
	 * 
	 * @return
	 */
	private JPanel createTopButtonBar() {
		executeButton = new ImageButton("Python", "execute", this);
		stopButton = new ImageButton("Python", "stop", this);
		openFileButton = new ImageButton("Python", "open", this);
		;
		saveFileButton = new ImageButton("Python", "save", this);
		;

		JPanel buttonBar = new JPanel();
		buttonBar.add(openFileButton);
		buttonBar.add(saveFileButton);
		buttonBar.add(stopButton);
		buttonBar.add(executeButton);

		return buttonBar;
	}

	/**
	 * Build up the top text menu bar.
	 * 
	 * @return the menu bar filled with the top-level options.
	 */
	private JMenuBar createTopMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("file");
		menuBar.add(fileMenu);
		fileMenu.setMnemonic(fileMenuMnemonic);
		createFileMenu(fileMenu);

		// examples -----
		JMenu examples = new JMenu("examples");
		menuBar.add(examples);
		examples.setMnemonic(examplesMenuMnemonic);
		createExamplesMenu(examples);

		return menuBar;
	}

	@Override
	public void detachGUI() {
		javaConsole.stopLogging();
		unsubscribe("publishState", "getState", Python.class);
		unsubscribe("finishedExecutingScript");
		unsubscribe("publishStdOut", "getStdOut", String.class);
		unsubscribe("appendScript", "appendScript", String.class);
		unsubscribe("startRecording", "startRecording", String.class);
	}

	/**
	 * 
	 */
	public void finishedExecutingScript() {
		executeButton.deactivate();
		stopButton.deactivate();
	}

	/**
	 * 
	 * @param j
	 */
	public void getState(Python j) {
		// TODO set GUIService state debug from Service data

	}

	/**
	 * 
	 * @param data
	 */
	public void getStdOut(String data) {
		pythonConsole.append(data);
	}

	/**
	 * 
	 */
	@Override
	public void init() {
		display.setLayout(new BorderLayout());
		display.setPreferredSize(new Dimension(800, 600));

		// --------- text menu begin ------------------------
		JPanel menuPanel = createMenuPanel();

		display.add(menuPanel, BorderLayout.PAGE_START);

		DefaultCaret caret = (DefaultCaret) pythonConsole.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		splitPane = createMainPane();

		display.add(splitPane, BorderLayout.CENTER);
		display.add(statusInfo, BorderLayout.PAGE_END);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.control.ServiceGUI#makeReadyForRelease() Shutting
	 * down - check for dirty script and offer to save
	 */
	@Override
	public void makeReadyForRelease() {

		log.info("makeReadyForRelease");

		// Iterator<String> it = scripts.keySet().iterator();
		Iterator<Entry<String, EditorPanel>> it = scripts.entrySet().iterator();
		while (it.hasNext()) {

			Map.Entry pairs = it.next();

			TextEditorPane e = ((EditorPanel) pairs.getValue()).getEditor();
			log.info(String.format("checking script %s", e.getFileFullPath()));
			if (e.isDirty()) {
				try {
					log.info(String.format("saving script / file %s", e.getFileFullPath()));
					e.save();
				} catch (Exception ex) {
					Logging.logError(ex);
				}
				/*
				 * FileLocation fl = FileLocation.create(e.getFileFullPath());
				 * String filename =
				 * JOptionPane.showInputDialog(myService.getFrame(),
				 * "Save File?", name); if (filename != null) { fl =
				 * FileLocation.create(filename); try { e.saveAs(fl); } catch
				 * (IOException e1) { Logging.logException(e1); // TODO
				 * Auto-generated catch block } }
				 */
			}
		}

	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent me) {
		// TODO Auto-generated method stub
		Object o = me.getSource();
		if (o instanceof TabControl2) {
			TabControl2 tc = (TabControl2) o;
			currentScriptName = tc.getText();
		}
		// log.info(me);
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 */
	private void openFile() {
		// TODO does this need to be closed?
		String newfile = FileUtil.open(top, "*.java");
		if (newfile != null) {
			// editor.setText(newfile);
			String filename = FileUtil.getLastFileOpened();
			Script script = new Script(filename, newfile);
			addNewEditorPanel(script);
			statusInfo.setText("Loaded: " + FileUtil.getLastFileOpened());
			return;
		}
		statusInfo.setText(FileUtil.getLastStatus());
		return;
	}

	/**
	 * Perform an execute action.
	 */
	private void performExecute() {
		executeButton.activate();
		stopButton.deactivate();
		javaConsole.startLogging(); // Hmm... noticed this is only local JVM
									// :) the Python console can be pushed
									// over the network
		if (scripts.containsKey(currentScriptName)) {
			EditorPanel p = scripts.get(currentScriptName);
			myService.send(boundServiceName, "exec", p.editor.getText());
		} else {
			log.error(String.format("cant exec %s", currentScriptName));
		}
	}

	/**
	 * Perform the restart action.
	 */
	private void performStop() {
		stopButton.activate();
		// executeButton.deactivate();
		myService.send(boundServiceName, "stop");
		myService.send(boundServiceName, "attachPythonConsole");
	}

	/**
	 * 
	 */
	public void saveAsFile() {
		if (scripts.containsKey(currentScriptName)) {
			EditorPanel p = scripts.get(currentScriptName);
			if (FileUtil.saveAs(top, p.editor.getText(), currentScriptName)) // FIXME
																				// -
																				// don't
																				// create
																				// new
																				// if
																				// unnecessary
			{
				currentScriptName = FileUtil.getLastFileSaved();
				scripts.remove(p);
				editorTabs.remove(p.panel);
				EditorPanel np = addNewEditorPanel(new Script(currentScriptName, p.editor.getText()));
				editorTabs.setSelectedComponent(np.panel);
			}
		} else {
			log.error(String.format("cant saveAsFile %s", currentScriptName));
		}
		// TODO do we need to handle errors with permissions?
	}

	/**
	 * 
	 */
	public void saveFile() {
		if (scripts.containsKey(currentScriptName)) {
			EditorPanel p = scripts.get(currentScriptName);
			if (FileUtil.save(top, p.editor.getText(), currentScriptName)) // FIXME
																			// -
																			// don't
																			// create
																			// new
																			// if
																			// unnecessary
			{
				currentScriptName = FileUtil.getLastFileSaved();
				scripts.remove(p);
				editorTabs.remove(p.panel);
				EditorPanel np = addNewEditorPanel(new Script(currentScriptName, p.editor.getText()));
				editorTabs.setSelectedComponent(np.panel);

				// sdfafafdds
			}
		} else {
			log.error(String.format("cant saveFile %s", currentScriptName));
		}

		// TODO do we need to handle errors with permissions?

	}

	public void startRecording(String filename) {
		addNewEditorPanel(new Script(filename, ""));
	}

}
