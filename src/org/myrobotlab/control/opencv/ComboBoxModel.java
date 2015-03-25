package org.myrobotlab.control.opencv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataListener;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ComboBoxModel implements MutableComboBoxModel, ActionListener {
	public final static Logger log = LoggerFactory.getLogger(ComboBoxModel.class.toString());

	private static final DefaultComboBoxModel model = new DefaultComboBoxModel();

	private static final HashMap<String, String> modelMap = new HashMap<String, String>();

	private static final HashMap<String, ArrayList<String>> possibleSources = new HashMap<String, ArrayList<String>>();

	private String selected;
	private OpenCVFilterGUI filterGUI;

	public static void addElement(String name, String source) {
		model.addElement(source);
		modelMap.put(name, name);
		ArrayList<String> possible;
		if (possibleSources.containsKey(name)) {
			possible = possibleSources.get(name);
		} else {
			possible = new ArrayList<String>();
			possibleSources.put(name, possible);
		}
		possible.add(source);
	}

	static public boolean contains(String key) {
		return modelMap.containsKey(key);
	}
	public static void removeSource(String name) {
		ArrayList<String> possible = possibleSources.get(name);
		modelMap.remove(name);
		for (String source : possible) {
			model.removeElement(source);
		}
	}

	public ComboBoxModel(OpenCVFilterGUI openCVFilterGUI) {
		this.filterGUI = openCVFilterGUI;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JComboBox input = (JComboBox) e.getSource();
		if ((String) input.getSelectedItem() != null) {
			filterGUI.boundFilter.filter.sourceKey = (String) input.getSelectedItem();
		}
	}

	@Override
	public void addElement(Object obj) {
		// modelMap.put((String)obj, (String)obj);
		model.addElement(obj);
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		model.addListDataListener(l);
	}

	@Override
	public Object getElementAt(int index) {
		return model.getElementAt(index);
	}

	@Override
	public Object getSelectedItem() {
		return selected;
	}

	@Override
	public int getSize() {
		return model.getSize();
	}

	@Override
	public void insertElementAt(Object obj, int index) {
		// modelMap.put((String)obj, (String)obj);
		model.insertElementAt(obj, index);
	}

	// FIXME FIXME FIXME - DANGEROUS & POSSIBLY WRONG - MABYE NOT SUPPORTED
	// REMOTELY !!!!
	// FIXME FIXME

	@Override
	public void removeElement(Object obj) {
		// modelMap.remove((String) obj);
		model.removeElement(obj);
	}

	@Override
	public void removeElementAt(int index) {
		String t = (String) getElementAt(index);
		if (t != null) {
			modelMap.remove(t);
		}
		model.removeElementAt(index);
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		model.removeListDataListener(l);
	}

	@Override
	public void setSelectedItem(Object anItem) {
		// TODO Auto-generated method stub
		selected = (String) anItem;
	}

	/*
	 * static public DefaultComboBoxModel getModel() { return model; }
	 */

}
