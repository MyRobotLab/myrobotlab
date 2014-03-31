package org.myrobotlab.dynamicGUI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.undo.*;
import javax.swing.event.*;
import javax.swing.text.*;

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