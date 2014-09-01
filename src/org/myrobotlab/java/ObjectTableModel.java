package org.myrobotlab.java;

import javax.swing.table.DefaultTableModel;

public class ObjectTableModel extends DefaultTableModel {
	int rows = 0;

	final String[] columnNames = { "#","Name", "Value" ,"Type"};

	public ObjectTableModel() {
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}
	
	 @Override
	    public boolean isCellEditable(int row, int column) {
	       //all cells false
	       return false;
	    }

}
