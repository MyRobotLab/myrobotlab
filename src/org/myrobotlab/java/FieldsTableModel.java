package org.myrobotlab.java;
import java.util.Hashtable;

import javax.swing.table.AbstractTableModel;

public class FieldsTableModel extends AbstractTableModel {
int rows=0;
Hashtable ht=new Hashtable();

final String[] columnNames ={"","Modifiers","Type","Name","Value","Parent Class"};
public FieldsTableModel(){}

public void add(String[] data){
 ht.put(new Integer(rows).toString(),data);
 rows++;
}

public void reset(){
  ht=new Hashtable();
  rows=0;
  fireTableDataChanged();	  
  
}

public int getRowCount(){
  return rows;
}

public int getColumnCount(){
  return 6;
}

public String getColumnName(int col) {
        return columnNames[col];
}

public Object getValueAt(int row, int column){
  String[] st=(String[])ht.get(new Integer(row).toString());
  return st[column];
}
} 
