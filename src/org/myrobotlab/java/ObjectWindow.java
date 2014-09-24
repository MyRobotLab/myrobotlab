package org.myrobotlab.java;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ObjectWindow extends JFrame {
	ObjectTableModel jfields = new ObjectTableModel();
	public ObjectTableModel jobjects = new ObjectTableModel();
	LinkedList<String> fieldList = new LinkedList<String>();
	LinkedList<String> objectList = new LinkedList<String>();
	final JTable fieldTable;
	final JTable objectTable;

	public ObjectWindow() {
		super("Objects");
		getContentPane().setLayout(
				new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane);
		fieldTable = new JTable(jfields);
		fieldTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fieldTable.setAutoCreateRowSorter(true);
		JScrollPane scrollPane = new JScrollPane(fieldTable);
		tabbedPane.addTab("Referenced Objects", null, scrollPane, null);

		objectTable = new JTable(jobjects);
		objectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		objectTable.setAutoCreateRowSorter(true);
		JScrollPane scrollPane_1 = new JScrollPane(objectTable);
		tabbedPane.addTab("Unreferenced Objects", null, scrollPane_1, null);

		fieldTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent arg0) {
						int selected = arg0.getLastIndex();
						System.out.println(selected);
						Object o=fieldTable.getValueAt(selected,2);
//						Agent.add("field"+fieldTable.getValueAt(selected,0),o);
					}
				});
		objectTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent arg0) {
						int selected = arg0.getLastIndex();
						Object o=objectTable.getValueAt(selected,2);
//						Agent.add((String)objectTable.getValueAt(selected,1),o);
					}
				});

		this.pack();
		this.setSize(400, 500);
		Dimension d67 = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d68 = this.getSize();
		this.setLocation((int) (d67.width / 2 - d68.width / 2),
				(int) (d67.height / 2 - d68.height / 2));
		BufferedImage bi = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
		Graphics g = bi.getGraphics();
		g.setColor(Color.yellow);
		g.fillRect(-20, -20, 50, 50);
		g.setColor(Color.red);
		g.setFont(new Font("Monospaced", Font.PLAIN, 70));
		g.drawString("*", -5, 45);
		this.setIconImage(bi);
		// this.setVisible(true);
		// this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				//Agent.mainFrame.btnObjects.setSelected(false);
			}
		});
	}

	public void fieldUpdate(final String name, final Object o) {
		if (fieldTable.getSelectedRow() > -1
				&& fieldTable.getSelectedRow() == fieldList.indexOf(name))
//			Agent.add("field"+fieldList.indexOf(name), o);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!fieldList.contains(name)) {
					fieldList.add(name);
					jfields.addRow(new Object[] { fieldList.indexOf(name),name, o,
							o.getClass().getCanonicalName() });
				} else
					jfields.setValueAt(o, fieldList.indexOf(name), 2);
			}
		});
	}

	public void ObjectUpdate(final String name, final Object o) {
					if (objectTable.getSelectedRow() > -1
				&& objectTable.getSelectedRow() == objectList.indexOf(name))
//			Agent.add(""+objectTable.getValueAt(objectTable.getSelectedRow() , 1), o);//"object" + objectList.indexOf(name)
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!objectList.contains(name)) {
					objectList.add(name);
					jobjects.addRow(new Object[] {objectList.indexOf(name), name,
							o, o.getClass().getCanonicalName() });
				} else
					jobjects.setValueAt(o, objectList.indexOf(name), 2);
			}
		});
	}

	public void removeObject(final String s) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				jobjects.removeRow(objectList.indexOf(s));
				objectList.remove(s);
			}
		});
	}
}
