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

package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.ivy.core.report.ResolveReport;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.net.SocketAppender;
import org.myrobotlab.control.widget.ProgressDialog;
import org.myrobotlab.control.widget.Style;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.framework.repo.Updates;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class RuntimeGUI extends ServiceGUI implements ActionListener {

	public final static Logger log = LoggerFactory.getLogger(RuntimeGUI.class);
	static final long serialVersionUID = 1L;

	HashMap<String, ServiceEntry> nameToServiceEntry = new HashMap<String, ServiceEntry>();
	JDialog updateDialog = null;
	ArrayList<String> resolveErrors = null;
	boolean localRepoChange = false;

	int popupRow = 0;

	JMenuItem installMenuItem = null;
	JMenuItem startMenuItem = null;
	JMenuItem upgradeMenuItem = null;
	JMenuItem releaseMenuItem = null;

	String possibleServiceFilter = null;
	ProgressDialog progressDialog = null;

	public Runtime myRuntime = null;

	DefaultListModel<ServiceEntry> currentServicesModel = new DefaultListModel<ServiceEntry>();
	DefaultTableModel possibleServicesModel = new DefaultTableModel() {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	CellRenderer cellRenderer = new CellRenderer();
	JTable possibleServices = new JTable(possibleServicesModel) {
		private static final long serialVersionUID = 1L;

		@Override
		public JToolTip createToolTip() {
			JToolTip tooltip = super.createToolTip();
			return tooltip;
		}

		// column returns content type
		public Class<?> getColumnClass(int column) {
			return getValueAt(0, column).getClass();
		}
	};

	JList runningServices = new JList(currentServicesModel);
	CurrentServicesRenderer currentServicesRenderer = new CurrentServicesRenderer();
	FilterListener filterListener = new FilterListener();
	JPopupMenu popup = new JPopupMenu();

	ServiceEntry releasedTarget = null;

	public RuntimeGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);

		// well done - this can be any runtime - which is good if there is
		// multiple instances
		myRuntime = (Runtime) Runtime.getService(boundServiceName);
	}

	public void init() {
		display.setLayout(new BorderLayout());

		progressDialog = new ProgressDialog(this);
		progressDialog.setVisible(false);

		getCurrentServices();

		runningServices.setCellRenderer(currentServicesRenderer);
		runningServices.setFixedCellWidth(200);

		possibleServicesModel.addColumn("");
		possibleServicesModel.addColumn("");
		possibleServices.setRowHeight(24);
		possibleServices.setIntercellSpacing(new Dimension(0, 0));
		possibleServices.setShowGrid(false);

		possibleServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		TableColumn col = possibleServices.getColumnModel().getColumn(1);
		col.setPreferredWidth(10);
		possibleServices.setPreferredScrollableViewportSize(new Dimension(300, 480));
		// set map to determine what types get rendered
		possibleServices.setDefaultRenderer(ImageIcon.class, cellRenderer);
		possibleServices.setDefaultRenderer(ServiceEntry.class, cellRenderer);
		possibleServices.setDefaultRenderer(String.class, cellRenderer);

		possibleServices.addMouseListener(new MouseAdapter() {
			// isPopupTrigger over OSs - use masks
			public void mouseReleased(MouseEvent e) {
				log.debug("mouseReleased");

				if (SwingUtilities.isRightMouseButton(e)) {
					log.debug("mouseReleased - right");
					popUpTrigger(e);
				}
			}

			public void popUpTrigger(MouseEvent e) {
				log.info("******************popUpTrigger*********************");
				JTable source = (JTable) e.getSource();
				popupRow = source.rowAtPoint(e.getPoint());
				ServiceEntry c = (ServiceEntry) possibleServicesModel.getValueAt(popupRow, 0);
				releaseMenuItem.setVisible(false);

				// "if" - the data already exists - (non transient) - then it
				// should be correct
				// for remote systems - but if it's "refreshed" it won't be
				// correct
				Repo repo = myRuntime.getRepo();
				ServiceData serviceData = repo.getServiceDataFile();
				//if (serviceData.hasUnfulfilledDependencies(c.type)) {
				if (!repo.isServiceTypeInstalled(c.type)) {
					// need to install it
					installMenuItem.setVisible(true);
					startMenuItem.setVisible(false);
					upgradeMenuItem.setVisible(false);
				} else {
					// have it
					installMenuItem.setVisible(false);
					startMenuItem.setVisible(true);
				}

				int column = source.columnAtPoint(e.getPoint());

				if (!source.isRowSelected(popupRow))
					source.changeSelection(popupRow, column, false, false);

				popup.show(e.getComponent(), e.getX(), e.getY());

			}

		});

		runningServices.addMouseListener(new MouseAdapter() {
			// isPopupTrigger over OSs - use masks
			public void mouseReleased(MouseEvent e) {
				log.debug("mouseReleased");

				if (SwingUtilities.isRightMouseButton(e)) {
					log.debug("mouseReleased - right");
					popUpTrigger(e);
				}
			}

			public void popUpTrigger(MouseEvent e) {
				log.info("******************popUpTrigger*********************");
				JList source = (JList) e.getSource();
				int index = source.locationToIndex(e.getPoint());
				if (index >= 0) {
					releasedTarget = (ServiceEntry) source.getModel().getElementAt(index);
					log.info(String.format("right click on running service %s", releasedTarget.name));
					releaseMenuItem.setVisible(true);
					upgradeMenuItem.setVisible(false);
					installMenuItem.setVisible(false);
					startMenuItem.setVisible(false);
				}
				popup.show(e.getComponent(), e.getX(), e.getY());

			}

		});

		JMenuItem menuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
		menuItem.setActionCommand("info");
		menuItem.setIcon(Util.getImageIcon("help.png"));
		menuItem.addActionListener(this);
		popup.add(menuItem);

		installMenuItem = new JMenuItem("install");
		installMenuItem.addActionListener(this);
		installMenuItem.setIcon(Util.getImageIcon("install.png"));
		// menuItem.setVisible(false);
		popup.add(installMenuItem);

		startMenuItem = new JMenuItem("start");
		startMenuItem.addActionListener(this);
		startMenuItem.setIcon(Util.getImageIcon("start.png"));
		// menuItem.setVisible(false);
		popup.add(startMenuItem);

		upgradeMenuItem = new JMenuItem("upgrade");
		upgradeMenuItem.addActionListener(this);
		upgradeMenuItem.setIcon(Util.getImageIcon("upgrade.png"));
		// menuItem.setVisible(false);
		popup.add(upgradeMenuItem);

		releaseMenuItem = new JMenuItem("release");
		releaseMenuItem.addActionListener(this);
		releaseMenuItem.setIcon(Util.getScaledIcon(Util.getImage("release.png"), 0.50));
		popup.add(releaseMenuItem);

		// getPossibleServices("all");

		JScrollPane runningServicesScrollPane = new JScrollPane(runningServices);
		JScrollPane possibleServicesScrollPane = new JScrollPane(possibleServices);

		runningServices.setVisibleRowCount(20);

		// make category filter buttons
		JPanel filters = new JPanel(new GridBagLayout());
		GridBagConstraints fgc = new GridBagConstraints();
		++fgc.gridy;
		fgc.fill = GridBagConstraints.HORIZONTAL;
		filters.add(new JLabel("category filters"), fgc);
		++fgc.gridy;
		JButton nofilter = new JButton("all");
		nofilter.addActionListener(filterListener);
		filters.add(nofilter, fgc);
		++fgc.gridy;

		Repo repo = myRuntime.getRepo();
		
		if (myRuntime != Runtime.getInstance()){
			log.info("foreign runtime");
		}

		ServiceData sd = repo.getServiceDataFile();
		String[] cats =sd.getCategoryNames();
		Arrays.sort(cats);

		for (int j = 0; j < cats.length; ++j) {
			JButton b = new JButton(cats[j]);
			b.addActionListener(filterListener);
			filters.add(b, fgc);
			++fgc.gridy;
		}

		JPanel possibleServicesPanel = new JPanel(new GridBagLayout());
		fgc.gridy = 0;
		fgc.gridx = 0;
		fgc.fill = GridBagConstraints.HORIZONTAL;
		possibleServicesPanel.add(new JLabel("possible services"), fgc);
		++fgc.gridy;
		possibleServicesPanel.add(possibleServicesScrollPane, fgc);

		JPanel runningServicesPanel = new JPanel(new GridBagLayout());
		fgc.gridy = 0;
		fgc.gridx = 0;
		fgc.fill = GridBagConstraints.HORIZONTAL;
		runningServicesPanel.add(new JLabel("running services"), fgc);
		++fgc.gridy;
		runningServicesPanel.add(runningServicesScrollPane, fgc);

		JPanel center = new JPanel();
		center.add(filters);
		center.add(possibleServicesPanel);
		center.add(runningServicesPanel);

		TitledBorder title;
		Platform platform = myRuntime.getPlatform();
		
		// TODO - get memory total & free - put in Platform? getMemory has to be implemented as a callback 
		title = BorderFactory.createTitledBorder(String.format("<html>%s %s</html>", platform.getPlatformId(), platform.getVersion()));
		center.setBorder(title);

		JMenuBar menuBar = new JMenuBar();
		JMenu system = new JMenu("system");
		menuBar.add(system);
		JMenu logging = new JMenu("logging");
		menuBar.add(logging);

		/*
		JMenuItem item = new JMenuItem("about");
		item.addActionListener(this);
		system.add(item);
		
		*/
		JMenuItem item = new JMenuItem("check for updates");
		item.addActionListener(this);
		system.add(item);

		item = new JMenuItem("install all");
		item.addActionListener(this);
		system.add(item);
		
		item = new JMenuItem("record");
		item.addActionListener(this);
		system.add(item);

		JMenu m1 = new JMenu("level");
		logging.add(m1);
		buildLogLevelMenu(m1);

		/*
		m1 = new JMenu("type");
		logging.add(m1);
		buildLogAppenderMenu(m1);
		*/

		display.add(menuBar, BorderLayout.NORTH);
		display.add(center, BorderLayout.CENTER);

	}

	public void getCurrentServices() {
		HashMap<String, ServiceInterface> services = Runtime.getRegistry();

		Map<String, ServiceInterface> sortedMap = null;
		sortedMap = new TreeMap<String, ServiceInterface>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceInterface sw = services.get(serviceName);
			if (sw.getInstanceId() != null) {
				ServiceEntry se = new ServiceEntry(serviceName, sw.getType(), true);
				currentServicesModel.addElement(se);
				nameToServiceEntry.put(serviceName, se);
			} else {
				ServiceEntry se = new ServiceEntry(serviceName, sw.getType(), false);
				currentServicesModel.addElement(se);
				nameToServiceEntry.put(serviceName, se);
			}
		}
	}

	// Called from GUIService - after making msg route from Runtime.registered
	public ServiceInterface registered(Service sw) {
		if (!nameToServiceEntry.containsKey(sw.getName())) {
			String typeName = (sw == null) ? "unknown" : sw.getType();
			ServiceEntry newServiceEntry = new ServiceEntry(sw.getName(), typeName, (sw.getInstanceId() != null));
			currentServicesModel.addElement(newServiceEntry);
			nameToServiceEntry.put(sw.getName(), newServiceEntry);
		}
		return sw;
	}

	public ServiceInterface released(Service sw) {
		// FIXME - bug if index is moved before call back is processed

		// myService.removeTab(sw.getName());// FIXME will bust when service ==
		// null
		if (nameToServiceEntry.containsKey(sw.getName())) {
			currentServicesModel.removeElement(nameToServiceEntry.get(sw.getName()));
		} else {
			log.error(sw.getName() + " released event - but could not find in currentServiceModel");
		}
		return sw;
	}

	@Override
	public void attachGUI() {

		// check to see if there are updates
		subscribe("checkingForUpdates", "checkingForUpdates");

		// results of checkForUpdates
		subscribe("publishUpdates", "publishUpdates", Updates.class);

		subscribe("updatesBegin", "updatesBegin", Updates.class);
		subscribe("updateProgress", "updateProgress", Status.class);
		subscribe("updatesFinished", "updatesFinished", ArrayList.class);

		// get the service info for the bound runtime (not necessarily local)
		subscribe("getServiceTypeNames", "onPossibleServicesRefresh", String[].class);

		getPossibleServices("all");
	}

	@Override
	public void detachGUI() {

		unsubscribe("checkingForUpdates", "checkingForUpdates");

		unsubscribe("publishUpdates", "publishUpdates", Updates.class);

		unsubscribe("updatesBegin", "updatesBegin", Updates.class);
		unsubscribe("updateProgress", "updateProgress", Status.class);
		unsubscribe("updatesFinished", "updatesFinished", ArrayList.class);

		// get the service info for the bound runtime (not necessarily local)
		unsubscribe("getServiceTypeNames", "onPossibleServicesRefresh", String[].class);
	}

	public void failedDependency(String dep) {
		JOptionPane.showMessageDialog(null, "<html>Unable to load Service...<br>" + dep + "</html>", "Error", JOptionPane.ERROR_MESSAGE);
	}

	class ServiceEntry {
		public String name;
		private String type;
		public boolean loaded = false;
		public boolean isRemote = false;

		ServiceEntry(String name, String type, boolean isRemote) {
			this.name = name;
			this.type = type;
			this.isRemote = isRemote;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getSimpleName() {
			if (type.indexOf(".") != -1) {
				return type.substring(type.lastIndexOf(".") + 1);
			} else {
				return type;
			}
		}

		public String toString() {
			return getSimpleName();
		}
	}

	class CurrentServicesRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		public CurrentServicesRenderer() {
			setOpaque(true);
			setIconTextGap(12);
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			// log.info("getListCellRendererComponent - begin");
			ServiceEntry entry = (ServiceEntry) value;

			setText("<html><font color=#" + Style.listBackground + ">" + entry.name + "</font></html>");

			// ImageIcon icon = Util.getScaledIcon(Util.getImage((entry.type +
			// ".png").toLowerCase(), "unknown.png"), 0.50);
			ImageIcon icon = Util.getScaledIcon(Util.getImage((entry.getSimpleName() + ".png"), "unknown.png"), 0.50);
			setIcon(icon);

			if (isSelected) {
				setBackground(Style.listHighlight);
				setForeground(Style.listBackground);
			} else {
				setBackground(Style.listBackground);
				setForeground(Style.listForeground);
			}

			// log.info("getListCellRendererComponent - end");
			return this;
		}
	}

	/**
	 * this is a request to the Runtime's ivy xml service data.
	 * 
	 * @param filter
	 */
	public void getPossibleServices(final String filter) {
		possibleServiceFilter = filter;
		myService.send(boundServiceName, "getServiceTypeNames", filter);
	}

	public void onPossibleServicesRefresh(final String[] serviceTypeNames) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (int i = possibleServicesModel.getRowCount(); i > 0; --i) {
					possibleServicesModel.removeRow(i - 1);
				}

				possibleServicesModel.getRowCount();

				// FIXME
				// String[] sscn = Runtime.getServiceTypeNames(filter);
				ServiceEntry[] ses = new ServiceEntry[serviceTypeNames.length];
				ServiceEntry se = null;

				for (int i = 0; i < ses.length; ++i) {
					// log.info("possible service {}", i);
					se = new ServiceEntry(null, serviceTypeNames[i], false);

					possibleServicesModel.addRow(new Object[] { se, "" });
				}

				possibleServicesModel.fireTableDataChanged();
				possibleServices.invalidate();
			}
		});
	}

	// FIXME - checkingForUpdates needs to process ? versus display current
	// ServiceTypes
	class CellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {

			Repo repo = myRuntime.getRepo();
			setEnabled(table == null || table.isEnabled());
			ServiceEntry entry = (ServiceEntry) table.getValueAt(row, 0);

			repo.getServiceDataFile();
			boolean availableToInstall = repo.isServiceTypeInstalled(entry.type);
			boolean upgradeAvailable = false;

			String upgradeString = "<html><h6>upgrade<br>";
			/*
			 * FIXME - process checkingForUpdates List<Dependency> deps =
			 * info.checkForUpgrade("org.myrobotlab.service." + entry.type); if
			 * (deps.size() > 0) { upgradeAvailable = true; for (int i = 0; i <
			 * deps.size(); ++i) { upgradeString += deps.get(i).getModule() +
			 * " " + deps.get(i).getRevision();
			 * 
			 * if (i < deps.size() - 1) { upgradeString += "<br>"; }
			 * 
			 * } upgradeString += "</h6></html>"; }
			 */

			// select by class being published by JTable on how to display
			if (value.getClass().equals(ServiceEntry.class)) {
				setHorizontalAlignment(JLabel.LEFT);
				setIcon(Util.getScaledIcon(Util.getImage((entry.getSimpleName() + ".png"), "unknown.png"), 0.50));
				setText(entry.getSimpleName());
				// setToolTipText("<html><body bgcolor=\"#E6E6FA\">" +
				// entry.type+
				// " <a href=\"http://myrobotlab.org\">blah</a></body></html>");

			} else if (value.getClass().equals(String.class)) {
				setIcon(null);
				setHorizontalAlignment(JLabel.LEFT);

				if (!availableToInstall) {
					setText("<html><h6>not<br>installed&nbsp;</h6></html>");
				} else {
					if (upgradeAvailable) {
						setText(upgradeString);
					} else {
						setText("<html><h6>latest&nbsp;</h6></html>");
					}
				}

			} else {
				log.error("unknown class");
			}

			if (possibleServices.isRowSelected(row)) {
				setBackground(Style.listHighlight);
				setForeground(Style.listForeground);
			} else {

				if (!availableToInstall) {
					setForeground(Style.listForeground);
					setBackground(Style.possibleServicesNotInstalled);
				} else {
					if (upgradeAvailable) {
						setForeground(Style.listForeground);
						setBackground(Style.possibleServicesUpdate);
					} else {
						setForeground(Style.listForeground);
						setBackground(Style.possibleServicesStable);
					}
				}
			}

			// setBorder(BorderFactory.createEmptyBorder());

			return this;
		}
	}

	class FilterListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent cmd) {
			log.info(cmd.getActionCommand());
			if ("all".equals(cmd.getActionCommand())) {
				getPossibleServices("");
			} else {
				getPossibleServices(cmd.getActionCommand());
			}
		}
	}

	public void checkingForUpdates() {
		// FIXME - if auto update or check on startup - we don't want a silly
		// dialog
		// we just want to be notified if there "is" an update - and whether or
		// not we
		// should apply it

		// FIXME - bypass is auto
		progressDialog.checkingForUpdates();
	}

	/**
	 * event method which is called when a "check for updates" request has new
	 * ServiceInfo data from the repo
	 * 
	 * @param si
	 * @return
	 */
	public void publishUpdates(Updates updates) {
		// depending on update changes options - no updates available

		progressDialog.publishUpdates(updates);
		// getPossibleServices("all");
	}
//zod
	@Override
	public void actionPerformed(ActionEvent event) {
		ServiceEntry c = (ServiceEntry) possibleServicesModel.getValueAt(popupRow, 0);
		String cmd = event.getActionCommand();
		Object o = event.getSource();
		
		if (releaseMenuItem == o) {
			myService.send(boundServiceName, "releaseService", releasedTarget.name);
			return;
		}

		if ("info".equals(cmd)) {
			BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + c.getSimpleName());
		} else if ("install".equals(cmd)) {
			int selectedRow = possibleServices.getSelectedRow();

			ServiceEntry entry = ((ServiceEntry) possibleServices.getValueAt(selectedRow, 0));
			Repo repo = myRuntime.getRepo();

			if (!repo.isServiceTypeInstalled(entry.getType())) {
				// dependencies needed !!!
				String msg = "<html>This Service has dependencies which are not yet loaded,<br>" + "do you wish to download them now?";
				JOptionPane.setRootFrame(myService.getFrame());
				int result = JOptionPane.showConfirmDialog(myService.getFrame(), msg, "alert", JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.CANCEL_OPTION) {
					return;
				}
				// you say "install", i say "update", repo says "resolve"
				myService.send(boundServiceName, "update", c.type);
			} else {
				// no unfulfilled dependencies - good to go
				addNewService(entry.getType());
			}

		} else if ("start".equals(cmd)) {
			int selectedRow = possibleServices.getSelectedRow();
			ServiceEntry entry = ((ServiceEntry) possibleServices.getValueAt(selectedRow, 0));
			addNewService(entry.getType());
			
		} else if ("install all".equals(cmd)) {
			myService.send(boundServiceName, "updateAll");
		} else if ("upgrade".equals(cmd)) {
			/* IMPORTANT - INSTALL OF A SERVICE */
			// send to "my" runtime - may be remote
			myService.send(myRuntime.getName(), "update", c.type);
		} else if ("check for updates".equals(cmd)) {
			myService.send(myRuntime.getName(), "checkForUpdates");
		} else if (cmd.equals(Level.DEBUG) || cmd.equals(Level.INFO) || cmd.equals(Level.WARN) || cmd.equals(Level.ERROR) || cmd.equals(Level.FATAL)) {
			Logging logging = LoggingFactory.getInstance();
			logging.setLevel(cmd);
		} else if (cmd.equals(Appender.FILE)) {
			Logging logging = LoggingFactory.getInstance();
			logging.addAppender(Appender.FILE);
		} else if (cmd.equals(Appender.CONSOLE)) {
			Logging logging = LoggingFactory.getInstance();
			logging.addAppender(Appender.CONSOLE);
		} else if (cmd.equals(Appender.NONE)) {
			Logging logging = LoggingFactory.getInstance();
			logging.removeAllAppenders();
			
		/*} else if (cmd.equals(Appender.REMOTE)) {
			JCheckBoxMenuItem m = (JCheckBoxMenuItem) ae.getSource();
			if (m.isSelected()) {
				ConnectDialog dlg = new ConnectDialog(new JFrame(), "connect to remote logging", "message", this, "127.0.0.1", "6767");
				Logging logging = LoggingFactory.getInstance();
				logging.addAppender(Appender.REMOTE, dlg.host.getText(), dlg.port.getText());
			} else {
				Logging logging = LoggingFactory.getInstance();
				logging.removeAppender(Appender.REMOTE);
			}
			*/
			
		} else {
			log.error("unknown command " + cmd);
		}

		// end actionCmd

	}

	public void addNewService(String newService) {
		JFrame frame = new JFrame();
		frame.setTitle("add new service");
		String name = JOptionPane.showInputDialog(frame, "new service name");

		if (name != null && name.length() > 0) {
			myService.send(boundServiceName, "createAndStart", name, newService);

		}
	}

	/**
	 * this is the beginning of the applyUpdates process
	 * 
	 * @param updates
	 * @return
	 */
	public Updates updatesBegin(Updates updates) {
		progressDialog.beginUpdates();
		return updates;
	}

	/**
	 * progress messages relating to an update
	 * 
	 * @param status
	 * @return
	 */
	public Status updateProgress(Status status) {
		// FIXME - start dialog - warn previously Internet connection necessary
		// no proxy

		if (status.isError()) {
			progressDialog.addErrorInfo(status.toString());
			if (resolveErrors == null) {
				resolveErrors = new ArrayList<String>();
			}
			resolveErrors.add(status.toString());
		} else {
			progressDialog.addInfo(status.toString());
		}
		return status;
	}

	/**
	 * updatesFinished - finished processing updates. Looking for confirmation
	 * of a restart or a no worky in case of errors
	 * 
	 * @param report
	 */
	public void updatesFinished(ArrayList<ResolveReport> report) {
		progressDialog.addInfo("finished processing updates ");
		progressDialog.finished();
	}

	public void getState(final Runtime runtime) {
		myRuntime = runtime;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// FIXME - change to "all" or "" - null is sloppy - system has
				// to upcast

			}
		});
	}

	/**
	 * a restart command will be sent to the appropriate runtime
	 */
	public void restart() {
		send("restart");
	}

	/**
	 * Add all options to the Log Appender menu.
	 * 
	 * @param parentMenu
	 */
	private void buildLogAppenderMenu(JMenu parentMenu) {
		Enumeration appenders = LogManager.getRootLogger().getAllAppenders();
		boolean console = false;
		boolean file = false;
		boolean remote = false;

		while (appenders.hasMoreElements()) {
			Object o = appenders.nextElement();
			if (o.getClass() == ConsoleAppender.class) {
				console = true;
			} else if (o.getClass() == FileAppender.class) {
				file = true;
			} else if (o.getClass() == SocketAppender.class) {
				remote = true;
			}

			log.info(o.getClass().toString());
		}

		JCheckBoxMenuItem mi = new JCheckBoxMenuItem(Appender.NONE);
		mi.setSelected(!console && !file && !remote);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(Appender.CONSOLE);
		mi.setSelected(console);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(Appender.FILE);
		mi.setSelected(file);
		mi.addActionListener(this);
		parentMenu.add(mi);

		/*
		mi = new JCheckBoxMenuItem(Appender.REMOTE);
		mi.setSelected(remote);
		mi.addActionListener(this);
		parentMenu.add(mi);
		*/
	}

	/**
	 * Add all options to the Log Level menu.
	 * 
	 * @param parentMenu
	 */
	private void buildLogLevelMenu(JMenu parentMenu) {
		ButtonGroup logLevelGroup = new ButtonGroup();

		String level = LoggingFactory.getInstance().getLevel();

		JRadioButtonMenuItem mi = new JRadioButtonMenuItem(Level.DEBUG);
		mi.setSelected(("DEBUG".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.INFO);
		mi.setSelected(("INFO".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.WARN);
		mi.setSelected(("WARN".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.ERROR);
		mi.setSelected(("ERROR".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.FATAL); // TODO - deprecate to WTF
													// :)
		mi.setSelected(("FATAL".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);
	}

}