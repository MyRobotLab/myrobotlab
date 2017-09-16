package org.myrobotlab.swing.vision;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataListener;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ComboBoxModel2 implements MutableComboBoxModel<String>, ActionListener {
	public final static Logger log = LoggerFactory.getLogger(ComboBoxModel2.class);

	static final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
	// private static Set<String> possibleSources = new TreeSet<String>();
	private String selected;
	private OpenCVFilterGui filterGui;

	public static void removeSource(String name) {
		model.removeElement(name);
	}

	public ComboBoxModel2(OpenCVFilterGui openCVFilterGui) {
		this.filterGui = openCVFilterGui;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JComboBox<String> input = (JComboBox<String>) e.getSource();
		if ((String) input.getSelectedItem() != null) {
			filterGui.boundFilter.filter.sourceKey = (String) input.getSelectedItem();
		}
	}

	static public void add(String obj) {
		model.addElement(obj);
	}

	@Override
	public void addElement(String obj) {
		model.addElement(obj);
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		model.addListDataListener(l);
	}

	@Override
	public String getElementAt(int index) {
		return model.getElementAt(index);
	}

	@Override
	public Object getSelectedItem() {
		return selected;
		// model.getSelectedItem();
	}

	@Override
	public int getSize() {
		return model.getSize();
	}

	@Override
	public void insertElementAt(String obj, int index) {
		model.insertElementAt(obj, index);
	}

	@Override
	public void removeElement(Object obj) {
		model.removeElement(obj);
	}

	@Override
	public void removeElementAt(int index) {
		model.removeElementAt(index);
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		model.removeListDataListener(l);
	}

	@Override
	public void setSelectedItem(Object anItem) {
		// model.setSelectedItem(anItem);
		selected = (String) anItem;
	}

}
