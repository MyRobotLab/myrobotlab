package org.myrobotlab.dynamicGUI;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class ScriptArea extends JScrollPane{
  final UndoManager undo = new UndoManager();
  JTextArea ta=new JTextArea("//Commands\n");
  
  
  public ScriptArea(){
    super();
	this.setViewportView(ta);
    Document doc = ta.getDocument();
    
    doc.addUndoableEditListener(new UndoableEditListener() {
        public void undoableEditHappened(UndoableEditEvent evt) {
            undo.addEdit(evt.getEdit());
        }
    });

    
    ta.getActionMap().put("Undo",
        new AbstractAction("Undo") {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (CannotUndoException e) {
                }
            }
        });
    ta.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
    
    ta.getActionMap().put("Redo",
        new AbstractAction("Redo") {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException e) {
                }
            }
        });
    ta.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
	
  }
  public String getText(){
    return ta.getText();
  }
  public void setText(String t){
    ta.setText(t);
  }
  public Component getTextArea(){
    return ta;
  }
}