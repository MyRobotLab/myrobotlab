package org.myrobotlab.java;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.myrobotlab.service.Java;

public class Reflector extends JFrame implements TreeSelectionListener,
		ActionListener {
	private static final long serialVersionUID = 1L;
	private final FieldsTableModel FieldsData;
	private final MethodsTableModel MethodsData;
	private DefaultMutableTreeNode rootNode;
	private DefaultMutableTreeNode mNode;
	private DefaultTreeModel treeModel;
	private Java java;
	final UndoManager undo = new UndoManager();
	String parent = "";
	String toplevel = "java.reflector.tempObject";
	Object o;
	String sref;

	JPanel Panel0;
	JLabel Class;
	JList Implements;
	JTabbedPane TabbedPane1;

	JPanel Panel3;
	final JList Constructors;

	JPanel Panel4;
	JTable Fields;

	JPanel Panel6;
	JTable Methods;

	JPanel Panel7;
	JTextArea Source;
	JTree tree;
	JSplitPane js;
	JLabel Super;
	private Object tempObject;
	private int tempInt;
	private float tempFloat;
	private double tempDouble;
	private char tempChar;
	private boolean tempBoolean;
	private long tempLong;
	private short tempShort;
	private byte tempByte;
	private String objectName;

	// private String tempObject1;
	// private java.lang.Class<? extends Object> objectType;

	/**
	 * @param java
	 */
	public Reflector(Java java) {
		super("Reflector");
		this.java = java;
		Panel0 = new JPanel();
		GridBagLayout gbPanel0 = new GridBagLayout();
		GridBagConstraints gbcPanel0 = new GridBagConstraints();
		Panel0.setLayout(gbPanel0);

		Class = new JLabel("class");
		gbcPanel0.gridx = 0;
		gbcPanel0.gridy = 0;
		gbcPanel0.gridwidth = 1;
		gbcPanel0.gridheight = 1;
		gbcPanel0.fill = GridBagConstraints.BOTH;
		gbcPanel0.weightx = 1;
		gbcPanel0.weighty = 0;
		gbcPanel0.anchor = GridBagConstraints.NORTH;
		gbPanel0.setConstraints(Class, gbcPanel0);
		Panel0.add(Class);

		Implements = new JList();
		JScrollPane scpImplements = new JScrollPane(Implements);
		gbcPanel0.gridx = 0;
		gbcPanel0.gridy = 3;
		gbcPanel0.gridwidth = 1;
		gbcPanel0.gridheight = 1;
		gbcPanel0.fill = GridBagConstraints.HORIZONTAL;
		gbcPanel0.weightx = 0;
		gbcPanel0.weighty = 0;
		gbcPanel0.anchor = GridBagConstraints.NORTH;
		gbPanel0.setConstraints(scpImplements, gbcPanel0);
		Panel0.add(scpImplements);

		TabbedPane1 = new JTabbedPane();

		Panel3 = new JPanel();
		GridBagLayout gbPanel3 = new GridBagLayout();
		GridBagConstraints gbcPanel3 = new GridBagConstraints();
		Panel3.setLayout(gbPanel3);

		Constructors = new JList();
		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() > 0) {
					String sel = (String) Constructors.getSelectedValue();
					// MAIN DELETED HERE
					// if
					// (sel.indexOf(" ")>-1)main.putSystemClipboardString(sel.substring(sel.indexOf(" ")+1));
				}
			}
		};
		Constructors.addMouseListener(mouseListener);

		JScrollPane scpConstructors = new JScrollPane(Constructors);
		gbcPanel3.gridx = 0;
		gbcPanel3.gridy = 0;
		gbcPanel3.gridwidth = 20;
		gbcPanel3.gridheight = 16;
		gbcPanel3.fill = GridBagConstraints.BOTH;
		gbcPanel3.weightx = 1;
		gbcPanel3.weighty = 1;
		gbcPanel3.anchor = GridBagConstraints.NORTH;
		gbPanel3.setConstraints(scpConstructors, gbcPanel3);
		Panel3.add(scpConstructors);
		TabbedPane1.addTab("Constructors", Panel3);

		Panel4 = new JPanel();
		GridBagLayout gbPanel4 = new GridBagLayout();
		GridBagConstraints gbcPanel4 = new GridBagConstraints();
		Panel4.setLayout(gbPanel4);
		FieldsData = new FieldsTableModel();
		Fields = new JTable(FieldsData);
		Fields.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionModel rowSM = Fields.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;

				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				if (lsm.isSelectionEmpty()) {
				} else {
					int selectedRow = lsm.getMinSelectionIndex();
					// MAIN DELETED HERE
					// main.putSystemClipboardString(parent+(String)FieldsData.getValueAt(selectedRow,3));
					// startreflect("((" + tempObject.getClass().getName() + ")"
					// + objectName + ")."
					// + FieldsData.getValueAt(selectedRow, 3));
					// startreflect(objectName+"."+FieldsData.getValueAt(selectedRow,3));
				}
			}
		});
		Fields.getColumnModel().getColumn(0).setMaxWidth(1);
		Fields.getColumnModel().getColumn(1).setPreferredWidth(20);
		Fields.getColumnModel().getColumn(2).setPreferredWidth(50);
		Fields.getColumnModel().getColumn(3).setPreferredWidth(50);
		Fields.getColumnModel().getColumn(4).setPreferredWidth(50);
		Fields.getColumnModel().getColumn(5).setPreferredWidth(5);
		JScrollPane scpFields = new JScrollPane(Fields);
		gbcPanel4.gridx = 0;
		gbcPanel4.gridy = 0;
		gbcPanel4.gridwidth = 20;
		gbcPanel4.gridheight = 16;
		gbcPanel4.fill = GridBagConstraints.BOTH;
		gbcPanel4.weightx = 1;
		gbcPanel4.weighty = 1;
		gbcPanel4.anchor = GridBagConstraints.NORTH;
		gbPanel4.setConstraints(scpFields, gbcPanel4);
		Panel4.add(scpFields);
		TabbedPane1.addTab("Fields", Panel4);

		Panel6 = new JPanel();
		GridBagLayout gbPanel6 = new GridBagLayout();
		GridBagConstraints gbcPanel6 = new GridBagConstraints();
		Panel6.setLayout(gbPanel6);
		MethodsData = new MethodsTableModel();
		Methods = new JTable(MethodsData);
		Methods.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		rowSM = Methods.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;

				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				if (lsm.isSelectionEmpty()) {
				} else {
					int selectedRow = lsm.getMinSelectionIndex();
					// MAIN DELETED HERE
					// main.putSystemClipboardString(parent
					// + (String) MethodsData.getValueAt(selectedRow, 3));
				}
			}
		});
		Methods.getColumnModel().getColumn(0).setMaxWidth(1);
		Methods.getColumnModel().getColumn(1).setPreferredWidth(20);
		Methods.getColumnModel().getColumn(2).setPreferredWidth(70);
		Methods.getColumnModel().getColumn(3).setPreferredWidth(70);
		Methods.getColumnModel().getColumn(4).setPreferredWidth(5);
		JScrollPane scpMethods = new JScrollPane(Methods);
		gbcPanel6.gridx = 0;
		gbcPanel6.gridy = 0;
		gbcPanel6.gridwidth = 20;
		gbcPanel6.gridheight = 16;
		gbcPanel6.fill = GridBagConstraints.BOTH;
		gbcPanel6.weightx = 1;
		gbcPanel6.weighty = 1;
		gbcPanel6.anchor = GridBagConstraints.NORTH;
		gbPanel6.setConstraints(scpMethods, gbcPanel6);
		Panel6.add(scpMethods);
		TabbedPane1.addTab("Methods", Panel6);

		Panel7 = new JPanel();
		GridBagLayout gbPanel7 = new GridBagLayout();
		GridBagConstraints gbcPanel7 = new GridBagConstraints();
		Panel7.setLayout(gbPanel7);

		Source = new JTextArea(2, 10);
		JScrollPane scpSource = new JScrollPane(Source);
		gbcPanel7.gridx = 0;
		gbcPanel7.gridy = 0;
		gbcPanel7.gridwidth = 20;
		gbcPanel7.gridheight = 16;
		gbcPanel7.fill = GridBagConstraints.BOTH;
		gbcPanel7.weightx = 1;
		gbcPanel7.weighty = 1;
		gbcPanel7.anchor = GridBagConstraints.NORTH;
		gbPanel7.setConstraints(scpSource, gbcPanel7);
		Panel7.add(scpSource);
		TabbedPane1.addTab("Source", Panel7);

		gbcPanel0.gridx = 0;
		gbcPanel0.gridy = 4;
		gbcPanel0.gridwidth = 1;
		gbcPanel0.gridheight = 1;
		gbcPanel0.fill = GridBagConstraints.BOTH;
		gbcPanel0.weightx = 1;
		gbcPanel0.weighty = 1;
		gbcPanel0.anchor = GridBagConstraints.SOUTH;
		gbPanel0.setConstraints(TabbedPane1, gbcPanel0);
		Panel0.add(TabbedPane1);
		rootNode = new DefaultMutableTreeNode("java.reflector.tempObject");
		treeModel = new DefaultTreeModel(rootNode);
		tree = new JTree(treeModel);
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setShowsRootHandles(true);
		tree.addTreeSelectionListener(this);
		tree.setForeground(new Color(0, 0, 0));
		JScrollPane scptree = new JScrollPane(tree);
		gbcPanel0.gridx = 0;
		gbcPanel0.gridy = 0;
		gbcPanel0.gridwidth = 1;
		gbcPanel0.gridheight = 5;
		gbcPanel0.fill = GridBagConstraints.BOTH;
		gbcPanel0.weightx = 1;
		gbcPanel0.weighty = 1;
		gbcPanel0.anchor = GridBagConstraints.NORTH;
		gbPanel0.setConstraints(scptree, gbcPanel0);

		Super = new JLabel("extends");
		gbcPanel0.gridx = 0;
		gbcPanel0.gridy = 1;
		gbcPanel0.gridwidth = 1;
		gbcPanel0.gridheight = 1;
		gbcPanel0.fill = GridBagConstraints.BOTH;
		gbcPanel0.weightx = 1;
		gbcPanel0.weighty = 0;
		gbcPanel0.anchor = GridBagConstraints.NORTH;
		gbPanel0.setConstraints(Super, gbcPanel0);
		Panel0.add(Super);

		// setDefaultCloseOperation(HIDE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				// Agent.mainFrame.btnReflector.setSelected(false);
			}
		});
		js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scptree, Panel0);
		setContentPane(js);
		pack();
		Dimension d67 = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d68 = this.getSize();
		this.setLocation((int) (d67.width / 2 - d68.width / 2),
				(int) (d67.height / 2 - d68.height / 2));
//		BufferedImage bi = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
//		Graphics g = bi.getGraphics();
//		g.setColor(Color.yellow);
//		g.fillRect(-20, -20, 50, 50);
//		g.setColor(Color.red);
//		g.setFont(new Font("Monospaced", Font.PLAIN, 70));
//		g.drawString("*", -5, 45);
		// this.setVisible(true);
		URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		this.setIconImage(img);
		Document doc = Source.getDocument();
		Fields.setAutoCreateRowSorter(true);
		Methods.setAutoCreateRowSorter(true);
		doc.addUndoableEditListener(new UndoableEditListener() {
			public void undoableEditHappened(UndoableEditEvent evt) {
				undo.addEdit(evt.getEdit());
			}
		});

		Source.getActionMap().put("Undo", new AbstractAction("Undo") {
			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canUndo()) {
						undo.undo();
					}
				} catch (CannotUndoException e) {
				}
			}
		});
		Source.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");

		Source.getActionMap().put("Redo", new AbstractAction("Redo") {
			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canRedo()) {
						undo.redo();
					}
				} catch (CannotRedoException e) {
				}
			}
		});
		Source.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
		// addObject(rootNode, this);
		// toplevel.add(this);
		// addObject(rootNode, java.getName());
		// Agent.interpret("import com.klemstinegroup.agent.Agent;");
		// Agent.interpret("import java.util.Arrays;");
		// Agent.interpret("import java.awt.font.*;");
		// Agent.interpret("import java.beans.*;");
		// Agent.interpret("import java.io.*;");
		// Agent.interpret("import java.lang.*;");
		// Agent.interpret("import java.lang.annotation.*;");
		// Agent.interpret("import java.lang.ref.*;");
		// Agent.interpret("import java.lang.reflect.*;");
		// Agent.interpret("import java.math.*;");
		// Agent.interpret("import java.net.*;");
		// Agent.interpret("import java.nio.*;");
		// Agent.interpret("import java.nio.channels.*;");
		// Agent.interpret("import java.nio.channels.spi.*;");
		// Agent.interpret("import java.nio.charset.*;");
		// Agent.interpret("import java.nio.charset.spi.*;");
		// Agent.interpret("import java.security.*;");
		// Agent.interpret("import java.security.acl.*;");
		// Agent.interpret("import java.security.cert.*;");
		// Agent.interpret("import java.security.interfaces.*;");
		// Agent.interpret("import java.security.spec.*;");
		// Agent.interpret("import java.sql.*;");
		// Agent.interpret("import java.text.*;");
		// Agent.interpret("import java.util.*;");
		// Agent.interpret("import java.util.concurrent.*;");
		// Agent.interpret("import java.util.concurrent.atomic.*;");
		// Agent.interpret("import java.util.concurrent.locks.*;");
		// Agent.interpret("import java.util.jar.*;");
		// Agent.interpret("import java.util.logging.*;");
		// Agent.interpret("import java.util.prefs.*;");
		// Agent.interpret("import java.util.regex.*;");
		// Agent.interpret("import java.util.zip.*;");
		// Agent.interpret("import javax.crypto.*;");
		// Agent.interpret("import javax.crypto.interfaces.*;");
		// Agent.interpret("import javax.crypto.spec.*;");
		// Agent.interpret("import javax.microedition.khronos.egl.*;");
		// Agent.interpret("import javax.microedition.khronos.opengles.*;");
		// Agent.interpret("import javax.net.*;");
		// Agent.interpret("import javax.net.ssl.*;");
		// Agent.interpret("import javax.security.auth.*;");
		// Agent.interpret("import javax.security.auth.callback.*;");
		// Agent.interpret("import javax.security.auth.login.*;");
		// Agent.interpret("import javax.security.auth.x500.*;");
		// Agent.interpret("import javax.security.cert.*;");
		// Agent.interpret("import javax.sql.*;");
		// Agent.interpret("import javax.xml.*;");
		// Agent.interpret("import javax.xml.datatype.*;");
		// Agent.interpret("import javax.xml.namespace.*;");
		// Agent.interpret("import javax.xml.parsers.*;");
		// Agent.interpret("import javax.xml.transform.*;");
		// Agent.interpret("import javax.xml.transform.dom.*;");
		// Agent.interpret("import javax.xml.transform.sax.*;");
		// Agent.interpret("import javax.xml.transform.stream.*;");
		// Agent.interpret("import javax.xml.validation.*;");
		// Agent.interpret("import javax.xml.xpath.*;");
		// Agent.interpret("import junit.framework.*;");
		// Agent.interpret("import junit.runner.*;");
		// Agent.interpret("import org.apache.http.*;");
		// Agent.interpret("import org.apache.http.auth.*;");
		// Agent.interpret("import org.apache.http.auth.params.*;");
		// Agent.interpret("import org.apache.http.client.*;");
		// Agent.interpret("import org.apache.http.client.entity.*;");
		// Agent.interpret("import org.apache.http.client.methods.*;");
		// Agent.interpret("import org.apache.http.client.params.*;");
		// Agent.interpret("import org.apache.http.client.protocol.*;");
		// Agent.interpret("import org.apache.http.client.utils.*;");
		// Agent.interpret("import org.apache.http.conn.*;");
		// Agent.interpret("import org.apache.http.conn.params.*;");
		// Agent.interpret("import org.apache.http.conn.routing.*;");
		// Agent.interpret("import org.apache.http.conn.scheme.*;");
		// Agent.interpret("import org.apache.http.conn.ssl.*;");
		// Agent.interpret("import org.apache.http.conn.util.*;");
		// Agent.interpret("import org.apache.http.cookie.*;");
		// Agent.interpret("import org.apache.http.cookie.params.*;");
		// Agent.interpret("import org.apache.http.entity.*;");
		// Agent.interpret("import org.apache.http.impl.*;");
		// Agent.interpret("import org.apache.http.impl.auth.*;");
		// Agent.interpret("import org.apache.http.impl.client.*;");
		// Agent.interpret("import org.apache.http.impl.conn.*;");
		// Agent.interpret("import org.apache.http.impl.conn.tsccm.*;");
		// Agent.interpret("import org.apache.http.impl.cookie.*;");
		// Agent.interpret("import org.apache.http.impl.entity.*;");
		// Agent.interpret("import org.apache.http.impl.io.*;");
		// Agent.interpret("import org.apache.http.io.*;");
		// Agent.interpret("import org.apache.http.message.*;");
		// Agent.interpret("import org.apache.http.params.*;");
		// Agent.interpret("import org.apache.http.protocol.*;");
		// Agent.interpret("import org.apache.http.util.*;");
		// Agent.interpret("import org.json.*;");
		// Agent.interpret("import org.w3c.dom.*;");
		// Agent.interpret("import org.w3c.dom.ls.*;");
		// Agent.interpret("import org.xml.sax.*;");
		// Agent.interpret("import org.xml.sax.ext.*;");
		// Agent.interpret("import org.xml.sax.helpers.*;");
		// Agent.interpret("import org.xmlpull.v1.*;");
		// Agent.interpret("import org.xmlpull.v1.sax2.*;");
		// toplevel();
	}

	// public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
	// Object child) {
	// DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
	// treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
	// return childNode;
	// }

	// public void toplevel() {
	// // Object[] vars = main.interpreter.getVariableNames().toArray();
	// Object[] vars = new Object[Agent.interpreter..getLocals().keySet()
	// .size()];
	// int counter = 0;
	// for (LocalVariable lv : Agent.interpreter.getLocals().keySet()) {
	// vars[counter++] = lv.declaredName();
	// }
	// java.util.List ltemp = Arrays.asList(vars);
	// Collections.sort(ltemp);
	// vars = ltemp.toArray();
	// for (int r = 0; r < vars.length; r++) {
	// // if (((String)vars[r]).startsWith(("field"))){
	// // int fieldno=Integer.parseInt(((String) vars[r]).substring(5));
	// // String fieldname=Agent.objectWindow.fieldList.get(fieldno);
	// //
	// Agent.interpret("com.klemstinegroup.agent.Agent.fields.put(\""+fieldname+"\","+vars[r]+");");
	// // }
	// // if (((String)vars[r]).startsWith(("object"))){
	// // int objectno=Integer.parseInt(((String) vars[r]).substring(6));
	// // String objectname=Agent.objectWindow.objectList.get(objectno);
	// //
	// Agent.interpret("com.klemstinegroup.agent.Agent.objects.put(\""+objectname+"\","+vars[r]+");");
	// // }
	//
	// if (!toplevel.contains(vars[r])) {
	// toplevel.add(vars[r]);
	// addObject(rootNode, (String) vars[r]);
	// }
	// }
	// }

	// public void add(String var) {
	// addObject(rootNode, var);
	// }

	public void classinfo(Object o) {
		if (o == null) {
			Class.setText("No Class Found!");
			blank();
			return;
		}
		Class.setText(Modifier.toString(new JavaClassInfo(o.getClass())
				.getModifiers()) + " class " + o.getClass().getName());
		Super.setText("extends "
				+ new JavaClassInfo(o.getClass()).getSuperclass().getName());
		ClassInfo[] interf = new JavaClassInfo(o.getClass()).getInterfaces();
		String[] data = new String[interf.length + 1];
		for (int r = 0; r < interf.length; r++) {
			data[r] = "implements " + interf[r].getName();
		}
		Implements.setListData(data);
	}

	public void constructorinfo(Object o, String sref) {
		Hashtable crtdhash = new Hashtable();
		ConstructorInfo[] consti = new JavaClassInfo(o.getClass())
				.getConstructors();
		String[] constarray = new String[consti.length];
		for (int r = 0; r < consti.length; r++) {
			ClassInfo[] classi = consti[r].getParameterTypes();
			String ok = Modifier.toString(consti[r].getModifiers());
			constarray[r] = o.getClass().getName() + "(";
			for (int r1 = 0; r1 < classi.length; r1++) {
				constarray[r] += classi[r1].getName();
				if (r1 < classi.length - 1)
					constarray[r] += ",";
			}
			constarray[r] += ")";
			ok += " " + constarray[r];
			crtdhash.put(constarray[r], ok);
		}
		java.util.List ltemp = Arrays.asList(constarray);
		Collections.sort(ltemp);
		constarray = (String[]) ltemp.toArray();
		String[] data = new String[constarray.length];
		for (int r = 0; r < consti.length; r++) {
			data[r] = (String) crtdhash.get(constarray[r]);
		}
		Constructors.setListData(data);
	}

	public void methodinfo(Object o, String sref) {
		MethodsData.reset();
		Hashtable crtdhash = new Hashtable();
		JavaClassInfo jcm = (JavaClassInfo) new JavaClassInfo(o.getClass());
		Vector methodvector = new Vector();
		boolean first = true;
		while (jcm != null) {
			MethodInfo[] methodi = jcm.getMethods();
			for (int r = 0; r < methodi.length; r++) {
				String[] data = new String[6];
				data[1] = Modifier.toString(methodi[r].getModifiers());
				data[2] = methodi[r].getReturnType().getName();
				data[4] = jcm.getName();
				String methodarray = methodi[r].getName() + "(";
				ClassInfo[] classi = methodi[r].getParameterTypes();
				for (int r1 = 0; r1 < classi.length; r1++) {
					methodarray += classi[r1].getName();
					if (r1 < classi.length - 1)
						methodarray += ",";
				}
				methodarray += ")";
				data[3] = methodarray;
				if (first) {
					methodarray = "*" + methodarray;
					data[0] = "*";
				}
				methodvector.add(methodarray);
				crtdhash.put(methodarray, data);
			}
			jcm = (JavaClassInfo) jcm.getSuperclass();
			first = false;
		}
		java.util.List ltemp = Arrays.asList(methodvector.toArray());
		Collections.sort(ltemp);
		Object[] methodsort = ltemp.toArray();
		for (int r = 0; r < methodsort.length; r++) {
			MethodsData.add((String[]) crtdhash.get(methodsort[r]));
		}
		MethodsData.fireTableDataChanged();

	}

	public void fieldinfo(Object o, String sref) {
		FieldsData.reset();
		JavaClassInfo jc = (JavaClassInfo) new JavaClassInfo(o.getClass());
		Vector fieldvector = new Vector();
		Hashtable crtdhash = new Hashtable();
		boolean first = true;
		while (jc != null) {
			FieldInfo[] tempf = jc.getFields();
			for (int r = 0; r < tempf.length; r++) {
				String[] data = new String[6];
				data[1] = Modifier.toString(tempf[r].getModifiers());
				data[2] = tempf[r].getType().getName();
				Object o1;
				String className = o.getClass().getName();
				if (className.indexOf('<') > -1)
					className = className.substring(0, className.indexOf("<"));
				// o1 = Agent.interpret(
				// "String.valueOf(" + sref + "." + tempf[r].getName()
				// + ");");
				o1 = java.interpret("((" + jc.getName() + ")" + sref + ")."
						+ tempf[r].getName());
				String fff = sref + "." + tempf[r].getName();
				if (o1 instanceof Exception) {
					o1 = java.interpret(className + "." + tempf[r].getName());
					// fff = className + "." + tempf[r].getName();
				}
				if (o1 instanceof Exception) {
					o1 = new String("???");
				}
				if (o1 == null) {
					o1 = new String("null");
				}
				data[4] = o1.toString();
				if (java.isArray(sref + "." + tempf[r].getName()))
					data[4] = java.interpret(
							"Arrays.toString(" + sref + "."
									+ tempf[r].getName() + ")").toString();
				if (java.isArray(className + "." + tempf[r].getName()))
					data[4] = java.interpret(
							"Arrays.toString(" + className + "."
									+ tempf[r].getName() + ")").toString();
				data[5] = jc.getName();

				data[3] = fff.substring(sref.length() + 1);
				if (first) {
					data[0] = "*";
					fff = "*" + fff;
				}
				fieldvector.add(fff);
				crtdhash.put(fff, data);
			}
			jc = (JavaClassInfo) jc.getSuperclass();
			first = false;
		}
		java.util.List ltemp = Arrays.asList(fieldvector.toArray());
		Collections.sort(ltemp);
		Object[] fieldsort = ltemp.toArray();
		boolean flag = false;
		DefaultMutableTreeNode mNode1 = mNode;
		if (mNode1 != null) {
			parent = (String) mNode.getUserObject() + ".";
			while (!mNode1.getUserObject().equals(toplevel)) {
				mNode1 = (DefaultMutableTreeNode) mNode1.getParent();
				parent = (String) mNode1.getUserObject() + "." + parent;
			}
		}
		for (int r = 0; r < fieldsort.length; r++) {
			FieldsData.add((String[]) crtdhash.get((String) fieldsort[r]));
			if (mNode != null && (mNode.isLeaf() || flag == true)) {
				String temp = (String) fieldsort[r];
				temp = temp.substring(temp.lastIndexOf(".") + 1);
				addObject(mNode, temp);
				flag = true;
			}
		}
		FieldsData.fireTableDataChanged();
	}

	public void reflect(Object o, String sref) {
		if (o == null) {
			blank();
			return;
		}
		this.o = o;
		this.sref = sref;
	}

	public void toplevel(Object o) {
		// rootNode = new DefaultMutableTreeNode("java.reflector.tempObject");
		mNode = rootNode;
		// DefaultMutableTreeNode mNode1 = mNode;
		// Enumeration<MutableTreeNode> jjj = rootNode.children();
		// while(jjj.hasMoreElements()){
		// treeModel.removeNodeFromParent(jjj.nextElement());
		// }
		rootNode.removeAllChildren();
		treeModel.reload();
		// addObject(rootNode, toplevel);
		reflect(o);
		tree.setSelectionRow(0);
		tree.expandPath(tree.getSelectionPath());

	}

	public void reflect(Object o) {
		this.tempObject = o;
		// addObject(rootNode, o.toString());
		startreflect("((Java)" + java.getName() + ").reflector.tempObject");
	}

	public void reflect(long o) {
		this.tempLong = o;
		startreflect("((Java)" + java.getName() + ").reflector.tempLong");
	}

	public void reflect(float o) {
		this.tempFloat = o;
		startreflect("((Java)" + java.getName() + ").reflector.tempFloat");
	}

	public void reflect(short o) {
		this.tempShort = o;
		startreflect("((Java)" + java.getName() + ").reflector.tempShort");
	}

	public void reflect(double o) {
		this.tempDouble = o;
		startreflect("((Java)" + java.getName() + ").reflector.tempDouble");
	}

	public void reflect(char o) {
		this.tempChar = o;
		startreflect("((Java)" + java.getName() + ").reflector.tempChar");
	}

	public void reflect(boolean o) {
		this.tempBoolean = o;
		startreflect("((Java)" + java.getName() + ").reflector.tempBoolean");
	}

	public void reflect(byte o) {
		this.tempByte = o;
		startreflect("((Java)" + java.getName() + ").reflector.tempByte");
	}

	public void reflect(int o) {
		this.tempInt = o;
		startreflect("((Java)" + java.getName() + ").reflector.tempInt");
	}

	public void startreflect(String name) {
		this.objectName = name;

		this.setVisible(true);
		Object p = java.interpret("((Java)" + java.getName()
				+ ").reflector.reflect(" + name + ",\"" + name + "\");");
		if (p instanceof Exception) {
			p = java.interpret(o.getClass().getName() + ".reflector.reflect("
					+ name + ",\"" + name + "\");");
		}
		// java.interpret("import "+o.getClass().getName()+";");
		// addObject(rootNode,strip(name));
		java.interpret("import " + o.getClass().getName() + ";");
		// Object
		// a=java.interpret("((Java)"+java.getName()+").reflector.tempObject1=DynaComp.decompile(\""+o.getClass().getName()+"\");");
		new Thread(new Runnable() {
			public void run() {
				Source.setText(DynaComp.decompile(o));
			}
		}).start();

		if (java.isPrimitive(name)) {
			Object p1 = java.interpret(name);
			String prim = "";
			if (p1 != null) {
				if (p1.getClass().toString().equals("class java.lang.Integer"))
					prim = "int";
				if (p1.getClass().toString().equals("class java.lang.Double"))
					prim = "double";
				if (p1.getClass().toString().equals("class java.lang.Short"))
					prim = "short";
				if (p1.getClass().toString().equals("class java.lang.Float"))
					prim = "float";
				if (p1.getClass().toString()
						.equals("class java.lang.Character"))
					prim = "char";
				if (p1.getClass().toString().equals("class java.lang.Boolean"))
					prim = "boolean";
				if (p1.getClass().toString().equals("class java.lang.Long"))
					prim = "long";
				if (p1.getClass().toString().equals("class java.lang.Byte"))
					prim = "byte";

				String name1 = "." + name;
				Class.setText(prim + " "
						+ name1.substring(name1.lastIndexOf('.') + 1) + " = "
						+ p1.toString());
			}
			blank();
		} else {
			classinfo(o);
			constructorinfo(o, sref);
			fieldinfo(o, sref);
			methodinfo(o, sref);
		}
	}

	private String strip(String name) {
		if (name.startsWith("(")) {
			name = name.substring(name.indexOf(')', 1) + 1);
			name = name.replaceAll("\\)", "");
		}
		return name;
	}

	public void blank() {
		MethodsData.reset();
		FieldsData.reset();
		Super.setText("");
		String[] data = { "" };
		Implements.setListData(data);
		Constructors.setListData(data);
		Source.setText("");
	}

	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
				.getLastSelectedPathComponent();
		if (node == null)
			return;

		String no = (String) node.getUserObject();
		mNode = node;
		DefaultMutableTreeNode mNode1 = mNode;
		parent = "";
		if (mNode1 != null) {
			while (!mNode1.getUserObject().equals(toplevel)) {
				mNode1 = (DefaultMutableTreeNode) mNode1.getParent();
				// parent = "((" + mNode1.getUserObject().getClass().getName()
				// + ")" + (String) mNode1.getUserObject() + ")." + parent;
				//
				parent = (String) mNode1.getUserObject() + "." + parent;
			}
			
//				if (!node.children().hasMoreElements()) {
					startreflect(expand(parent + no));
					// tree.setSelectionPath(tree.getLeadSelectionPath());
					tree.expandPath(tree.getSelectionPath());
//				}
		}
	}

	public String expand(String s) {
		String[] sp = s.split("\\.");
		String full = "";
		for (String g : sp) {
			String d = full.equals("") ? g : (full + "." + g);
			
			Object p = java.interpret(d);
			if (p instanceof Exception) {
				p = java.interpret(full);
				String gh = p.getClass().getName();
				p = java.interpret(gh + "." + g);
				if (!(p instanceof Exception)) {
					full = gh + "." + g;
				}
			} else
				full = "((" + p.getClass().getName() + ")" + d + ")";
		}
		// if (full.length()>0)full=full.substring(0,full.length()-1);
		return full;
	}

	@Override
	public void actionPerformed(ActionEvent paramActionEvent) {
	}

	public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
			Object child) {
		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
		treeModel.insertNodeInto(childNode, parent, parent.getChildCount());
		return childNode;
	}
}