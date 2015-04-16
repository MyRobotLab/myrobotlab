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

package org.myrobotlab.control.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.control.ServiceGUI;
import org.myrobotlab.control.TabControl2;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Python;

/**
 * Editor
 * 
 * General purpose swing editor TODO generalize for Python & Arduino
 * 
 * @author GroG
 * 
 */
public class Editor extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	final static int fileMenuMnemonic = KeyEvent.VK_F;
	static final int saveMenuMnemonic = KeyEvent.VK_S;
	static final int openMenuMnemonic = KeyEvent.VK_O;
	static final int examplesMenuMnemonic = KeyEvent.VK_X;

	final JFrame top;

	final protected RSyntaxTextArea textArea;
	JScrollPane editorScrollPane;
	final JTabbedPane editorTabs;

	public JProgressBar progress = new JProgressBar(0, 100);

	JSplitPane splitPane;

	final JLabel statusLabel;
	public final JLabel status;

	// TODO - check for outside modification with lastmoddate
	File currentFile;
	String currentFilename;

	// menu
	JMenu fileMenu = createFileMenu();
	JMenu editMenu = createEditMenu();
	JMenu examplesMenu = new JMenu("Examples");;
	JMenu toolsMenu = createToolsMenu();
	JMenu helpMenu = createHelpMenu();

	JMenuBar menuBar = createMenuBar();
	JPanel buttonBar = new JPanel();

	JPanel menuPanel = createMenuPanel();

	// button bar buttons
	ImageButton executeButton;
	ImageButton restartButton;
	ImageButton openFileButton;
	ImageButton saveFileButton;

	// consoles
	JTabbedPane consoleTabs;
	public final Console console;

	// autocompletion
	//final CompletionProvider provider;
	//final AutoCompletion ac;

	String syntaxStyle;

	/**
	 * Constructor
	 * 
	 * @param boundServiceName
	 * @param myService
	 */
	public Editor(final String boundServiceName, final GUIService myService, final JTabbedPane tabs, String syntaxStyle) {
		super(boundServiceName, myService, tabs);

		this.syntaxStyle = syntaxStyle;

		console = new Console();

		//provider = createCompletionProvider();
		//ac = new AutoCompletion(provider);

		// FYI - files are on the "Arduino" service not on the GUIService -
		// these
		// potentially are remote objects
		currentFile = null;
		currentFilename = null;

		textArea = new RSyntaxTextArea();
		editorScrollPane = null;
		editorTabs = new JTabbedPane();

		splitPane = null;

		statusLabel = new JLabel("Status:");
		status = new JLabel("");
		top = myService.getFrame();

		textArea.getInputMap().put(KeyStroke.getKeyStroke("F3"), "find-action");
		textArea.getInputMap().put(KeyStroke.getKeyStroke("ctrl F"), "find-action");
		/*
		 * RSyntaxTextAreaFindAndReplaceable findAndReplaceable = new
		 * RSyntaxTextAreaFindAndReplaceable(); editArea.getActionMap().put(
		 * "find-action", new FindAndReplaceDialog( findAndReplaceable ) );
		 */
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		Object o = arg0.getSource();
		if (o == restartButton) {
			performRestart();
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
		if (m.getText().equals("Save")) {
			saveFile();
		} else if (m.getText().equals("Open")) {
			openFile();
		} else if (m.getText().equals("Save As")) {
			saveAsFile();
		} else if (m.getText().equals("Find")) {
			new FindAndReplaceDialog(this);
		}
		/*
		 * else if (m.getActionCommand().equals("examples")) {
		 * textArea.setText(FileIO
		 * .getResourceFile(String.format("python/examples/%1$s",
		 * m.getText()))); }
		 */
	}

	public ImageButton addImageButtonToButtonBar(String resourceDir, String name, ActionListener al) {
		ImageButton ret = new ImageButton(resourceDir, name, al);
		buttonBar.add(ret);
		return ret;
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Python.class);
		subscribe("finishedExecutingScript");
		subscribe("publishStdOut", "getStdOut", String.class);
		// myService.send(boundServiceName, "broadcastState");
	}

	CompletionProvider createCompletionProvider() {
		// TODO -> LanguageSupportFactory.get().register(editor);

		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		return new JavaCompletionProvider();
	}

	JMenu createEditMenu() {
		editMenu = new JMenu("Edit");
		editMenu.add(createMenuItem("Undo"));
		editMenu.add(createMenuItem("Redo"));
		editMenu.addSeparator();
		editMenu.add(createMenuItem("Cut"));
		editMenu.add(createMenuItem("Copy"));
		// editMenu.add(createMenuItem("save", saveMenuMnemonic, "control S",
		// null));
		editMenu.addSeparator();
		editMenu.add(createMenuItem("Find", openMenuMnemonic, "CTRL+F", "Find"));
		// editMenu.add(createMenuItem("Format"));
		return editMenu;
	}

	/**
	 * Build the editor pane.
	 * 
	 * @return
	 */
	JScrollPane createEditorPane() {
		textArea.setSyntaxEditingStyle(syntaxStyle);
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);

		// autocompletion
		//ac.install(textArea);
		//ac.setShowDescWindow(true);

		return new RTextScrollPane(textArea);
	}

	JMenu createFileMenu() {
		JMenu menu = new JMenu("File");
		menu.setMnemonic(fileMenuMnemonic);
		menu.add(createMenuItem("New"));
		menu.add(createMenuItem("Save", saveMenuMnemonic, "control S", null));
		menu.add(createMenuItem("Save As"));
		menu.add(createMenuItem("Open", openMenuMnemonic, "control O", null));
		menu.addSeparator();
		return menu;
	}

	JMenu createHelpMenu() {
		helpMenu = new JMenu("Help");
		return helpMenu;
	}

	/**
	 * Build the main portion of the view.
	 * 
	 * @return
	 */
	JSplitPane createMainPane() {
		JSplitPane pane = new JSplitPane();

		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BorderLayout());

		consoleTabs = createTabsPane();
		lowerPanel.add(consoleTabs, BorderLayout.CENTER);
		progress.setForeground(Color.green);
		lowerPanel.add(progress, BorderLayout.SOUTH);
		editorScrollPane = createEditorPane();

		pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, lowerPanel);
		pane.setDividerLocation(440);

		return pane;
	}

	/**
	 * Build up the top text menu bar.
	 * 
	 * @return the menu bar filled with the top-level options.
	 */
	JMenuBar createMenuBar() {
		menuBar = new JMenuBar();

		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(examplesMenu);
		menuBar.add(toolsMenu);
		menuBar.add(createHelpMenu());

		return menuBar;
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @return
	 */
	JMenuItem createMenuItem(String label) {
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
	JMenuItem createMenuItem(String label, int vKey, String accelerator, String actionCommand) {
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
	JMenuItem createMenuItem(String label, String actionCommand) {
		return createMenuItem(label, -1, null, actionCommand);
	}

	JPanel createMenuPanel() {

		JPanel menuPanel = new JPanel(new BorderLayout());
		menuPanel.add(menuBar, BorderLayout.LINE_START);
		menuPanel.add(buttonBar);

		return menuPanel;
	}

	JTabbedPane createTabsPane() {
		JTabbedPane pane = new JTabbedPane();
		pane.addTab("console", console.getScrollPane());
		pane.setTabComponentAt(pane.getTabCount() - 1, new TabControl2(self, pane, console.getScrollPane(), "console"));

		return pane;
	}

	JMenu createToolsMenu() {
		toolsMenu = new JMenu("Tools");
		return toolsMenu;
	}

	@Override
	public void detachGUI() {
		console.stopLogging();
		unsubscribe("publishStdOut", "getStdOut", String.class);
		unsubscribe("finishedExecutingScript");
		unsubscribe("publishState", "getState", Python.class);
	}

	public void finishedExecutingScript() {
		executeButton.deactivate();
	}

	public void getState(Service j) {
		// TODO set GUIService state debug from Service data

	}

	public RSyntaxTextArea getTextArea() {
		return textArea;
	}

	@Override
	public void init() {
		display.setLayout(new BorderLayout());
		display.setPreferredSize(new Dimension(800, 600));

		// default text based menu
		display.add(menuPanel, BorderLayout.PAGE_START);

		splitPane = createMainPane();

		display.add(splitPane, BorderLayout.CENTER);

		JPanel s = new JPanel(new FlowLayout(FlowLayout.LEFT));
		s.add(statusLabel);
		s.add(status);
		display.add(s, BorderLayout.PAGE_END);
	}

	/**
	 * 
	 */
	void openFile() {
		// TODO does this need to be closed?
		String newfile = FileUtil.open(top, "*.py");
		if (newfile != null) {
			textArea.setText(newfile);
			statusLabel.setText("Loaded: " + FileUtil.getLastFileOpened());
			return;
		}
		statusLabel.setText(FileUtil.getLastStatus());
		return;
	}

	/**
	 * Perform an execute action.
	 */
	void performExecute() {
		executeButton.activate();
		restartButton.deactivate();
		console.startLogging(); // Hmm... noticed this is only local JVM
								// :) the Python console can be pushed
								// over the network
		myService.send(boundServiceName, "attachPythonConsole");
		myService.send(boundServiceName, "exec", textArea.getText());
	}

	/**
	 * Perform the restart action.
	 */
	void performRestart() {
		restartButton.activate();
		executeButton.deactivate();
		myService.send(boundServiceName, "stop");
	}

	/**
	 * 
	 */
	void saveAsFile() {
		// TODO do we need to handle errors with permissions?
		if (FileUtil.saveAs(top, textArea.getText(), currentFilename))
			currentFilename = FileUtil.getLastFileSaved();
	}

	/**
	 * 
	 */
	void saveFile() {
		// TODO do we need to handle errors with permissions?
		if (FileUtil.save(top, textArea.getText(), currentFilename))
			currentFilename = FileUtil.getLastFileSaved();
	}

	public void setStatus(String s) {
		status.setText(s);
	}
}
