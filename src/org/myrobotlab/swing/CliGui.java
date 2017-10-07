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

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Cli;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class CliGui extends ServiceGui implements KeyListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(CliGui.class);

	JTextArea console = new JTextArea();
	JScrollPane scrollPane = new JScrollPane(console);
	StringBuilder input = new StringBuilder();

	public CliGui(final String boundServiceName, final SwingGui myService) {
		super(boundServiceName, myService);

		DefaultCaret caret = (DefaultCaret) console.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		console.setBackground(Color.BLACK);
		console.setForeground(Color.GREEN);
		add(scrollPane);
		console.addKeyListener(this);
	}

	public void keyPressed(KeyEvent e) {
		log.info("keyPressed {} {} {}", e.getKeyCode(), e.getKeyChar(), input.length());
		if (e.getKeyChar() == KeyEvent.VK_ALT) {
			// e.consume();
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE) {
			try {
			  if (input.length() - 2 < 0){
			    return;
			  }
				input.delete(input.length() - 2, input.length() - 1);
				/*
				Document doc = console.getDocument();
				doc.remove(doc.getLength() - 1, 1);
				*/
			} catch (Exception e2) {
				log.error("doc", e2);
			}
			// e.consume();
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			send("process", input.toString());
			input.setLength(0);
			// console.append("\n");
			// e.consume();
			return;
		}

		if (e.getKeyCode() > 31 && e.getKeyCode() < 128) {
			input.append(e.getKeyChar());			
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		/*
		 * log.info("keyTyped {} {}", e.getKeyChar(), e.getKeyCode()); if
		 * (e.getKeyCode() == KeyEvent.VK_UNDEFINED){ e.consume(); return; }
		 * input.append(e.getKeyChar()); int len =
		 * console.getDocument().getLength(); console.setCaretPosition(len);
		 */
	}

	public void onPrompt(String out) {
		console.append(out);
		console.setCaretPosition(console.getDocument().getLength());
	}

	public void onState(Cli cli) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

			}
		});
	}

	public void onStdout(String out) {
		console.append(out);
		console.setCaretPosition(console.getDocument().getLength());
	}

	@Override
	public void subscribeGui() {
		subscribe("stdout");
		subscribe("getPrompt");
		send("getPrompt");
	}

	@Override
	public void unsubscribeGui() {
		unsubscribe("stdout");
		unsubscribe("getPrompt");
	}

}
