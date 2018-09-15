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

package org.myrobotlab.swing;

import java.awt.BorderLayout;
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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.Script;
import org.myrobotlab.swing.widget.Console;
import org.myrobotlab.swing.widget.DockableTabPane;
import org.myrobotlab.swing.widget.EditorPanel;
import org.myrobotlab.swing.widget.FileUtil;
import org.myrobotlab.swing.widget.ImageButton;

/**
 * Python SwingGui
 * 
 * @author SwedaKonsult
 * 
 *         use - http://famfamfam.com/lab/icons/silk/previews/index_abc.png -
 *         SILK ICONS
 */
public class PythonGui extends ServiceGui implements ActionListener, MouseListener {

	static final long serialVersionUID = 1L;

	final JFrame jframe;

	// TODO - check for outside modification with lastmoddate
	// String currentScriptName;

	JMenu examples;

	ImageButton execute;
	ImageButton stop;
	ImageButton open;
	ImageButton save;

	// editors
	final DockableTabPane editorTabs = new DockableTabPane();
	final HashMap<String, EditorPanel> scripts = new HashMap<String, EditorPanel>();

	// consoles
	// GAP final JTabbedPane consoleTabs;
	final Console javaConsole;
	final JTextArea pythonConsole;
	final DockableTabPane console = new DockableTabPane();

	// auto-completion
	static CompletionProvider provider;

	static AutoCompletion ac;

	int untitledCount = 1;

	/*
	 * Constructor
	 * 
	 */
	public PythonGui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);

		javaConsole = new Console();
		pythonConsole = new JTextArea();

		// autocompletion - in the constructor so that they can be declared
		// final
		// provider = createCompletionProvider(); FIXME - takes forever
		// ac = new AutoCompletion(provider);

		provider = null;
		ac = null;

		jframe = myService.getFrame();

		Script s = new Script(String.format("%s%suntitled.%d.py", Service.getCfgDir(), File.separator, untitledCount),
				"");
		addNewEditorPanel(s);

		JPanel menuPanel = createMenuPanel();

		display.setLayout(new BorderLayout());
		display.add(menuPanel, BorderLayout.PAGE_START);

		DefaultCaret caret = (DefaultCaret) pythonConsole.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		console.addTab("python", new JScrollPane(pythonConsole));
		console.addTab("java", javaConsole.getScrollPane());

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorTabs.getTabs(), console.getTabs());
		splitPane.setDividerLocation(350);

		display.add(splitPane, BorderLayout.CENTER);

	}

	public String getSelected() {
		JTabbedPane tabs = editorTabs.getTabs();
		int index = tabs.getSelectedIndex();
		if (index < 0 || index > tabs.getTabCount()) {
			return null;
		}
		return tabs.getTitleAt(tabs.getSelectedIndex());
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		Object o = arg0.getSource();
		if (o == stop) {
			stop.activate();
			send("stop");
			send("attachPythonConsole");
			return;
		} else if (o == execute) {
			execute.activate();
			stop.deactivate();
			javaConsole.startLogging(); // Hmm... noticed this is only local JVM
			// :) the Python console can be pushed
			// over the network
			String currentScriptName = getSelected();
			if (scripts.containsKey(currentScriptName)) {
				EditorPanel p = scripts.get(currentScriptName);
				send("exec", p.getText());
			} else {
				log.error("can't exec {}", currentScriptName);
			}
			return;
		} else if (o == save) {
			save();
			return;
		} else if (o == open) {
			openFile();
			return;
		}

		if (!(o instanceof JMenuItem)) {
			return;
		}
		JMenuItem m = (JMenuItem) o;
		if (m.getText().equals("new")) {
			++untitledCount;
			Script s = new Script(
					String.format("%s%suntitled.%d.py", Service.getCfgDir(), File.separator, untitledCount), "");
			addNewEditorPanel(s);
		} else if (m.getText().equals("save")) {
			save();
		} else if (m.getText().equals("open")) {
			openFile();
		} else if (m.getText().equals("save as")) {
			saveAs();
		} else if (m.getText().equals("close")) {
			closeFile();
		} else if (m.getActionCommand().equals("examples")) {
			// } else if (m.getActionCommand().equals("examples")) {

			// BareBonesBrowserLaunch.openURL("https://github.com/MyRobotLab/pyrobotlab");
			/*
			 * String filename = String.format("Python/examples/%1$s",
			 * m.getText()); Script script = new Script(filename,
			 * FileIO.resourceToString(filename)); addNewEditorPanel(script);
			 */
		}
	}

	public EditorPanel addNewEditorPanel(Script script) {
		EditorPanel panel = new EditorPanel(script);
		editorTabs.addTab(script.getName(), panel.getDisplay());
		scripts.put(script.getName(), panel);
		return panel;
	}
	
	public void onAppendScript(Script data) {
		String currentScriptName = getSelected();
		if (currentScriptName == null) {
			error("no script selected to append");
			return;
		}
		EditorPanel p = scripts.get(currentScriptName);
		if (p != null) {
			p.setText(String.format("%s\n%s", p.getText(), data.getCode()));
		} else {
			info("can't append Script to current");
		}
	}

	@Override
	public void subscribeGui() {
		subscribe("publishState");
		subscribe("finishedExecutingScript");
		subscribe("publishStdOut");
		subscribe("appendScript");
		// subscribe("publishLoadedScript", "addNewEditorPanel");
		send("attachPythonConsole");
	}

	@Override
	public void unsubscribeGui() {
		javaConsole.stopLogging();
		unsubscribe("publishState");
		unsubscribe("finishedExecutingScript");
		unsubscribe("publishStdOut");
		unsubscribe("appendScript");
		// unsubscribe("publishLoadedScript", "addNewEditorPanel");
	}

	public void closeFile() {
		String currentScriptName = getSelected();
		if (scripts.containsKey(currentScriptName)) {
			EditorPanel p = scripts.get(currentScriptName);
			if (p.isDirty()) {
				saveAs();
			}

			p = scripts.get(currentScriptName);
			scripts.remove(p);
			editorTabs.removeTab(currentScriptName);

		} else {
			log.error("can't closeFile {}", currentScriptName);
		}
	}

	public JMenuItem createMenuItem(String label) {
		return createMenuItem(label, -1, null, null);
	}

	public JMenuItem createMenuItem(String label, int vKey, String accelerator, String actionCommand) {
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

	/*
	 * Build the top menu panel.
	 * 
	 */
	public JPanel createMenuPanel() {
		JMenuBar menuBar = createTopMenuBar();
		JPanel buttonBar = createTopButtonBar();

		JPanel menuPanel = new JPanel(new BorderLayout());
		menuPanel.add(menuBar, BorderLayout.LINE_START);
		menuPanel.add(buttonBar);

		return menuPanel;
	}

	public JPanel createTopButtonBar() {
		execute = new ImageButton("Python", "execute", this);
		stop = new ImageButton("Python", "stop", this);
		open = new ImageButton("Python", "open", this);
		save = new ImageButton("Python", "save", this);

		JPanel buttonBar = new JPanel();
		buttonBar.add(open);
		buttonBar.add(save);
		buttonBar.add(stop);
		buttonBar.add(execute);

		return buttonBar;
	}

	public JMenuBar createTopMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("file");
		menuBar.add(fileMenu);
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(createMenuItem("new"));
		fileMenu.add(createMenuItem("save", KeyEvent.VK_S, "control S", null));
		fileMenu.add(createMenuItem("save as"));
		fileMenu.add(createMenuItem("open", KeyEvent.VK_O, "control O", null));
		fileMenu.add(createMenuItem("close"));
		fileMenu.addSeparator();

		examples = new JMenu("examples");
		menuBar.add(examples);
		examples.setMnemonic(KeyEvent.VK_X);
		examples.addMouseListener(this);

		return menuBar;
	}

	/**
	 * 
	 */
	public void onFinishedExecutingScript() {
		execute.deactivate();
		stop.deactivate();
	}

	public void onState(final Python python) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// merge state with view
				/*
				 * Script script = python.getScript(); if (script != null) { if
				 * (!scripts.containsKey(script.getName())) {
				 * addNewEditorPanel(script); } }
				 */

			}
		});

	}

	public void onStdOut(final String data) {
		/** REMOVE IF FLAKEY BUGS APPEAR !! */
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pythonConsole.append(data);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.swing.widget.ServiceGUI#makeReadyForRelease() Shutting
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
			log.info("checking script {}", e.getFileFullPath());
			if (e.isDirty()) {
				try {
					log.info("saving script / file {}", e.getFileFullPath());
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
		if (o == examples) {
			BareBonesBrowserLaunch.openURL("https://github.com/MyRobotLab/pyrobotlab/tree/master/service");
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void openFile() {
		// TODO does this need to be closed?
		String newfile = FileUtil.open(jframe, "*.py");
		if (newfile != null) {
			// editor.setText(newfile);
			String filename = FileUtil.getLastFileOpened();
			Script script = new Script(filename, newfile);
			addNewEditorPanel(script);
			info("Loaded: " + FileUtil.getLastFileOpened());
			return;
		}
		info(FileUtil.getLastStatus());
		return;
	}

	public void saveAs() {
		String currentScriptName = getSelected();
		if (scripts.containsKey(currentScriptName)) {
			EditorPanel p = scripts.get(currentScriptName);
			// FIXME - don't create if not necessary
			if (FileUtil.saveAs(jframe, p.getText(), currentScriptName)) {
				currentScriptName = FileUtil.getLastFileSaved();
				scripts.remove(p);
				editorTabs.removeTab(currentScriptName);
				EditorPanel np = addNewEditorPanel(new Script(currentScriptName, p.getText()));
				editorTabs.setSelectedComponent(np.getDisplay());
			}
		} else {
			log.error("cant saveAsFile {}", currentScriptName);
		}
		// TODO do we need to handle errors with permissions?
	}

	public void save() {
		String oldName = getSelected();
		EditorPanel p = scripts.get(oldName);
		FileUtil.save(jframe, p.getText(), oldName);
		/*
		if (scripts.containsKey(oldName)) {
			EditorPanel p = scripts.get(oldName);
			if (FileUtil.save(jframe, p.getText(), oldName)) {

				String currentScriptName = FileUtil.getLastFileSaved();
				scripts.remove(p);
				editorTabs.remove(p.getDisplay());
				EditorPanel np = addNewEditorPanel(new Script(currentScriptName, p.getText()));
				editorTabs.setSelectedComponent(np.getDisplay());
				log.info("here");
				
			}
		} else {
			log.error(String.format("cant saveFile %s", oldName));
		}
		*/
	}

}
