package org.myrobotlab.swing.widget;

import java.awt.Container;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JScrollPane;

import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.data.Script;

public class EditorPanel {
	String filename;
	TextEditorPane editor;
	JScrollPane panel;

	public EditorPanel(Script script) {
		try {
			filename = script.getName();
			editor = new TextEditorPane(RTextArea.INSERT_MODE, false, FileLocation.create(new File(filename)));
			editor.setText(script.getCode());
			editor.setCaretPosition(0);
			
			// editor tweaks
			editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
			editor.setCodeFoldingEnabled(true);
			editor.setAntiAliasingEnabled(true);

			// auto-completion
			/*
			if (ac != null) {
				ac.install(editor);
				ac.setShowDescWindow(true);
			}
			*/
			
			panel = new RTextScrollPane(editor);
			
		} catch (Exception e) {
			Logging.logError(e);
		}
	}



	public String getTitle() {
		if (filename.startsWith("Python/examples/")) {

			return filename.substring("Python/examples/".length());

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

	public String getText() {
		return editor.getText();
	}

	public Container getDisplay() {		
		return panel;
	}

	public void setText(String text) {
		editor.setText(text);
	}
	
	public boolean isDirty() {
		return editor.isDirty();
	}

	public Icon getIcon() {		
		return null;
	}

	public String getToolTip() {
		return "tool tip";
	}
}

