package org.myrobotlab.java;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class ThreadViewer extends JFrame {
	private ThreadViewerTableModel tableModel;

	public ThreadViewer() {
		super("Threads");
		tableModel = new ThreadViewerTableModel();

		JTable table = new JTable(tableModel);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionModel rowSM = table.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				if (lsm.isSelectionEmpty()) {
				} else {
					int sr = lsm.getMinSelectionIndex();
					if (JOptionPane.showConfirmDialog(new JFrame(),
							"Are you sure you want to kill thread - "
									+ tableModel.thread[sr].getName() + "?",
							"Quit", 2) == 0) {
						tableModel.thread[sr].stop();
					}
				}
			}
		});
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		TableColumnModel colModel = table.getColumnModel();
		int numColumns = colModel.getColumnCount();

		// manually size all but the last column
		for (int i = 0; i < numColumns - 1; i++) {
			TableColumn col = colModel.getColumn(i);

			col.sizeWidthToFit();
			col.setPreferredWidth(col.getWidth() + 5);
			col.setMaxWidth(col.getWidth() + 5);
		}

		JScrollPane sp = new JScrollPane(table);

		setLayout(new BorderLayout());
		add(sp, BorderLayout.CENTER);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				//Agent.mainFrame.btnThreads.setSelected(false);
			}
		});

		setContentPane(sp);
		setSize(500, 300);
		BufferedImage bi = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
		Graphics g = bi.getGraphics();
		g.setColor(Color.yellow);
		g.fillRect(-20, -20, 50, 50);
		g.setColor(Color.red);
		g.setFont(new Font("Monospaced", Font.PLAIN, 70));
		g.drawString("*", -5, 45);
		setIconImage(bi);
		Dimension d67 = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d68 = getSize();
		setLocation((int) (d67.width / 2 - d68.width / 2),
				(int) (d67.height / 2 - d68.height / 2));
	}

	public void dispose() {
		tableModel.stopRequest();
	}

	protected void finalize() throws Throwable {
		dispose();
	}


}
