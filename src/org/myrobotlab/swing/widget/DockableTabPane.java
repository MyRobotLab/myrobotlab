package org.myrobotlab.swing.widget;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.myrobotlab.framework.interfaces.StateSaver;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * generalized dockable tab panel
 * 
 * <pre>
 * panel parts are
 * 		title
 * 		icon
 * 		toolTip
 * 		enabled
 * 		mnemonic
 * </pre>
 *
 */

public class DockableTabPane implements ActionListener {
	public final static Logger log = LoggerFactory.getLogger(DockableTabPane.class);

	/**
	 * we need 2 indexes/containers for our tabs one is the swing JTabbedPane,
	 * but this is not useful in its serializable form so we maintain a 'custom'
	 * map of dockable tabs - this contains size and location info which is
	 * serializable
	 */
	transient JTabbedPane tabs = new JTabbedPane();
	
	/**
	 * a interface / callback to preserve location, dimensions,
	 * and other data of a desktop
	 */
	transient StateSaver stateSaver;

	/**
	 * serializable map of dockable tabs - used for saving locations and
	 * positions of undocked panels to preserve coordinates in saved desktops
	 */
	Map<String, Map<String, DockableTabData>> desktops;
	
	public String currentDesktop;

	transient Map<String, DockableTab> dockableTabs = new TreeMap<String, DockableTab>();

	public DockableTabPane(StateSaver stateSaver) {
		this.stateSaver = stateSaver;
		tabs.setMinimumSize(new Dimension(0, 0));
		
		if (currentDesktop == null){
			currentDesktop = "default";
		}
		
		// if desktops wasn't saved  by the service
		if (desktops == null){
			desktops = new TreeMap<String, Map<String, DockableTabData>> ();
		}
		
		if (!desktops.containsKey(currentDesktop)){
			setDesktop(currentDesktop);
		}
	}

	public DockableTabPane() {
		this(null);
	}
	
	public void addTab(String title, Component display){
		addTab(title, display, null);	
	}
	
	// FIXME - set preffered size ??
	public void addTab(String title, Component display, String tip) {
		if (dockableTabs.containsKey(title)) {
			log.info("addTab - {} already contains tab", title);
			return;
		}
		tabs.addTab(title, null, display, tip);
		DockableTab newTab = new DockableTab(this, title, display);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, newTab.getTitleLabel());
		dockableTabs.put(title, newTab);
		if (desktops.get(currentDesktop).containsKey(title)){			
			newTab.setData(desktops.get(currentDesktop).get(title));
		} else {
			desktops.get(currentDesktop).put(title, newTab.getData());
		}
		// tabs.setPreferredSize(new Dimension(300, 300));
	}

	public JTabbedPane getTabs() {
		return tabs;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
	}

	public void setTabPlacementRight() {
		tabs.setTabPlacement(SwingConstants.RIGHT);
	}

	public void removeTab(String title) {
		if (dockableTabs.containsKey(title)) {
			DockableTab tab = dockableTabs.get(title);
			tabs.remove(tab.display);
		}
	}

	public void setSelectedComponent(Container component) {
		tabs.setSelectedComponent(component);
	}

	public void dockTab(String title) {
		if (dockableTabs.containsKey(title)) {
			dockableTabs.get(title).dockTab();
		}
	}

	public void hideTab(String title) {
		if (dockableTabs.containsKey(title)) {
			dockableTabs.get(title).hideTab();
		}
	}

	public void undockTab(String title) {
		if (dockableTabs.containsKey(title)) {
			dockableTabs.get(title).undockTab();
		}
	}

	public void unhideTab(String title) {
		if (dockableTabs.containsKey(title)) {
			dockableTabs.get(title).unhideTab();
		}
	}

	public Set<String> keySet() {
		return dockableTabs.keySet();
	}

	public DockableTab get(String title) {
		return dockableTabs.get(title);
	}

	public int size() {
		int z = tabs.getTabCount();
		return dockableTabs.size();
	}

	public void setDesktop(String name) {
		currentDesktop = name;
		if (!desktops.containsKey(currentDesktop)) {
			Map<String, DockableTabData> desktop = resetDesktop(currentDesktop);
		} else {
			
		}
	}

	public Map<String, DockableTabData> resetDesktop(String name) {
		Map<String, DockableTabData> desktop = new TreeMap<String, DockableTabData>();
		desktops.put(name, desktop);
		return desktop;
	}

	public void removeDesktop(String name) {
		if (desktops.containsKey(name)) {
			desktops.remove(name);
		}
	}

	public void save() {
		if (stateSaver != null){
			stateSaver.save();
		}
	}

	public void setStateSaver(StateSaver stateSaver) {
		this.stateSaver = stateSaver;
	}

	public void remove(Container display) {
		tabs.remove(display);
	}

	public void explode() {
		for (String key : dockableTabs.keySet()) {
			undockTab(key);
		}
	}
	
	public void collapse() {
		for (String key : dockableTabs.keySet()) {
			dockTab(key);
		}
	}
}
