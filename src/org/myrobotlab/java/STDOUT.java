package org.myrobotlab.java;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

/**
 * Class STDOUT creates a Java Console for GUIService based Java Applications. Once
 * created, a Console component receives all the data directed to the standard
 * output (System.out) and error (System.err) streams.
 * <p>
 * For example, once a Java Console is created for an application, data passed
 * on to any methods of System.out (e.g., System.out.println(" ")) and
 * System.err (e.g., stack trace in case of uncought exceptions) will be
 * received by the Console.
 * <p>
 * Note that a Java Console can not be created for Applets to run on any
 * browsers due to security violations. Browsers will not let standard output
 * and error streams be redicted (for obvious reasons).
 * 
 * @author Subrahmanyam Allamaraju (sallamar@cvimail.cv.com)
 */
public class STDOUT extends JFrame implements StreamObserver {
	JTextArea aTextArea;
	JTextArea bTextArea;
	JScrollPane js;

	ObservableStream errorDevice;
	ObservableStream outputDevice;

	ByteArrayOutputStream _errorDevice;
	ByteArrayOutputStream _outputDevice;

	PrintStream errorStream;
	PrintStream outputStream;

	PrintStream _errorStream;
	PrintStream _outputStream;

	JButton clear;
	JToggleButton bottomLock;
	protected boolean scrollFlag=true;

	/**
	 * Creates a Java Console.
	 */
	public STDOUT() {
		super("Output");

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				resetOutput();
				resetError();
				//Agent.mainFrame.btnOutput.setSelected(false);
			}
		});

		aTextArea = new JTextArea();
		aTextArea.setEditable(true);

		clear = new JButton("Clear");
		bottomLock = new JToggleButton("Lock");
		bottomLock.setSelected(true);

		clear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aTextArea.setText("");
			}
		});

		bottomLock.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//setVisible(false);
				//resetOutput();
				//resetError();
				scrollFlag=!scrollFlag;
				bottomLock.setSelected(scrollFlag);
			}
		});
		Panel buttonPanel = new Panel();
		buttonPanel.setLayout(new GridLayout(1, 0));
		buttonPanel.add(clear);
		buttonPanel.add(bottomLock);

		this.getContentPane().setLayout(new BorderLayout());
		js = new JScrollPane(aTextArea);
		this.getContentPane().add("Center", js);
		this.getContentPane().add("South", buttonPanel);
		js.getVerticalScrollBar().addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent paramMouseEvent) {
			}

			@Override
			public void mousePressed(MouseEvent paramMouseEvent) {
				scrollFlag=false;
				bottomLock.setSelected(scrollFlag);
			}

			@Override
			public void mouseReleased(MouseEvent paramMouseEvent) {
				if (js.getVerticalScrollBar().getValue() > js.getVerticalScrollBar()
						.getMaximum()
						- js.getVerticalScrollBar().getVisibleAmount()
						- 10) {
					scrollFlag = true;
					bottomLock.setSelected(scrollFlag);
				}
			}

			@Override
			public void mouseEntered(MouseEvent paramMouseEvent) {
			}

			@Override
			public void mouseExited(MouseEvent paramMouseEvent) {
			}});

		_errorStream = System.err;
		_outputStream = System.out;

		_outputDevice = new ByteArrayOutputStream();
		_errorDevice = new ByteArrayOutputStream();

		BufferedImage bi = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
		Graphics g = bi.getGraphics();
		g.setColor(Color.yellow);
		g.fillRect(-20, -20, 50, 50);
		g.setColor(Color.red);
		g.setFont(new Font("Monospaced", Font.PLAIN, 70));
		g.drawString("*", -5, 45);
		this.setIconImage(bi);

		this.pack();
		Dimension d67 = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d68 = this.getSize();
		this.setLocation((int) (d67.width / 2 - d68.width / 2),
				(int) (d67.height / 2 - d68.height / 2));
		this.setError();
		this.setOutput();
		//this.setVisible(true);
	}

	/**
	 * Clears the Console.
	 */
	public void clear() {
		try {
			outputDevice.writeTo(_outputDevice);
		} catch (IOException e) {
		}
		outputDevice.reset();

		try {
			errorDevice.writeTo(_errorDevice);
		} catch (IOException e) {
		}
		errorDevice.reset();

		aTextArea.setText("");
	}

	/**
	 * Sets the error device to the Console if not set already.
	 * 
	 * @see #resetError
	 */
	public final void setError() {
		errorDevice = new ObservableStream();
		errorDevice.addStreamObserver(this);

		errorStream = new PrintStream(errorDevice, true);

		System.setErr(errorStream);
	}

	/**
	 * Resets the error device to the default. Console will no longer receive
	 * data directed to the error stream.
	 * 
	 * @see #setError
	 */
	public final void resetError() {
		System.setErr(_errorStream);
	}

	/**
	 * Sets the output device to the Console if not set already.
	 * 
	 * @see #resetOutput
	 */
	public final void setOutput() {
		outputDevice = new ObservableStream();
		outputDevice.addStreamObserver(this);

		outputStream = new PrintStream(outputDevice, true);

		System.setOut(outputStream);
	}

	/**
	 * Resets the output device to the default. Console will no longer receive
	 * data directed to the output stream.
	 * 
	 * @see #setOutput
	 */
	public final void resetOutput() {
		System.setOut(_outputStream);
	}

	/**
	 * Gets the minimumn size.
	 */
	public Dimension getMinimumSize() {
		return new Dimension(500, 800);
	}

	/**
	 * Gets the preferred size.
	 */
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	public void change(JTextArea ta) {
		bTextArea = aTextArea;
		aTextArea = ta;
	}

	public void changeback() {
		aTextArea = bTextArea;
	}

	public void streamChanged() {
		// aTextArea.append(js.getVerticalScrollBar().getValue()+" "+(js.getVerticalScrollBar().getMaximum()
		// - js.getVerticalScrollBar().getVisibleAmount())+"\n");
		aTextArea.append(outputDevice.toString());
		try {
			outputDevice.writeTo(_outputDevice);
		} catch (IOException e) {
		}
		outputDevice.reset();

		errorStream.checkError();
		aTextArea.append(errorDevice.toString());
		try {
			errorDevice.writeTo(_errorDevice);
		} catch (IOException e) {
		}
		errorDevice.reset();
		if (scrollFlag) {
			aTextArea.setCaretPosition(aTextArea.getText().length());
			js.getVerticalScrollBar().setValue( js.getVerticalScrollBar().getMaximum() );
//			aTextArea.invalidate();
//			js.invalidate();
		}
	}

	/**
	 * Returns contents of the error device directed to it so far. Calling <a
	 * href="#clear">clear</a> has no effect on the return data of this method.
	 */
	public ByteArrayOutputStream getErrorContent() throws IOException {
		ByteArrayOutputStream newStream = new ByteArrayOutputStream();
		_errorDevice.writeTo(newStream);

		return newStream;
	}

	/**
	 * Returns contents of the output device directed to it so far. Calling <a
	 * href="#clear">clear</a> has no effect on the return data of this method.
	 */
	public ByteArrayOutputStream getOutputContent() throws IOException {
		ByteArrayOutputStream newStream = new ByteArrayOutputStream();
		_outputDevice.writeTo(newStream);

		return newStream;
	}
}
