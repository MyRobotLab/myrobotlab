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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.ivy.core.report.ResolveReport;
import org.myrobotlab.control.widget.ProgressDialog;
import org.myrobotlab.control.widget.Style;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Category;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.framework.repo.ServiceType;
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

	// FIXME - checkingForUpdates needs to process ? versus display current
	// ServiceTypes
	class CellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {

			Repo repo = myRuntime.getRepo();
			setEnabled(table == null || table.isEnabled());
			repo.getServiceDataFile();
			Boolean availableToInstall = null;

			boolean upgradeAvailable = false;

			String upgradeString = "<html><h6>upgrade<br>";

			// select by class being published by JTable on how to display
			if (value.getClass().equals(ServiceType.class)) {
				ServiceType entry = (ServiceType) table.getValueAt(row, 0);
				setHorizontalAlignment(SwingConstants.LEFT);
				setIcon(Util.getScaledIcon(Util.getImage((entry.getSimpleName() + ".png"), "unknown.png"), 0.50));
				setText(entry.getSimpleName());
				// setToolTipText("<html><body bgcolor=\"#E6E6FA\">" +
				// entry.type+
				// " <a href=\"http://myrobotlab.org\">blah</a></body></html>");

			} else if (value instanceof ServiceInterface) {
				ServiceInterface entry = (ServiceInterface) table.getValueAt(row, 0);
				setHorizontalAlignment(SwingConstants.LEFT);
				setIcon(Util.getScaledIcon(Util.getImage((entry.getSimpleName() + ".png"), "unknown.png"), 0.50));
				setText(entry.getSimpleName());

			} else if (value.getClass().equals(String.class)) {
				ServiceType entry = (ServiceType) table.getValueAt(row, 0);
				availableToInstall = repo.isServiceTypeInstalled(entry.name);

				setIcon(null);
				setHorizontalAlignment(SwingConstants.LEFT);

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

				ServiceType entry = (ServiceType) table.getValueAt(row, 0);
				availableToInstall = repo.isServiceTypeInstalled(entry.name);

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

	class CurrentServicesRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		public CurrentServicesRenderer() {
			setOpaque(true);
			setIconTextGap(12);
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			ServiceInterface entry = (ServiceInterface) value;
			setText("<html><font color=#" + Style.listBackground + ">" + entry.getName() + "</font></html>");

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

	class FilterListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent cmd) {
			log.info(cmd.getActionCommand());
			if ("all".equals(cmd.getActionCommand())) {
				getPossibleServices();
			} else {
				getPossibleServices(cmd.getActionCommand());
			}
		}
	}

	public final static Logger log = LoggerFactory.getLogger(RuntimeGUI.class);
	static final long serialVersionUID = 1L;
	HashMap<String, ServiceInterface> nameToServiceEntry = new HashMap<String, ServiceInterface>();

	JDialog updateDialog = null;

	ArrayList<String> resolveErrors = null;
	boolean localRepoChange = false;
	int popupRow = 0;
	JMenuItem infoMenuItem = null;
	JMenuItem installMenuItem = null;

	JMenuItem startMenuItem = null;
	JMenuItem upgradeMenuItem = null;

	JMenuItem releaseMenuItem = null;
	String possibleServiceFilter = null;
	ProgressDialog progressDialog = null;

	public Runtime myRuntime = null;
	public Repo myRepo = null;

	public ServiceData serviceData = null;

	DefaultListModel<ServiceInterface> currentServicesModel = new DefaultListModel<ServiceInterface>();
	DefaultListModel<JButton> filterButtonModel = new DefaultListModel<JButton>();
	JList<ServiceInterface> runningServices = new JList<ServiceInterface>(currentServicesModel);

	CurrentServicesRenderer currentServicesRenderer = new CurrentServicesRenderer();

	FilterListener filterListener = new FilterListener();

	JPopupMenu popup = new JPopupMenu();

	ServiceInterface releasedTarget = null;

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
		@Override
		public Class<?> getColumnClass(int column) {
			return getValueAt(0, column).getClass();
		}
	};

	public RuntimeGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);

		// well done - this can be any runtime - which is good if there is
		// multiple instances
		myRuntime = (Runtime) Runtime.getService(boundServiceName);
		myRepo = myRuntime.getRepo();
		serviceData = myRepo.getServiceData();
	}

	// zod
	@Override
	public void actionPerformed(ActionEvent event) {
		ServiceType c = (ServiceType) possibleServicesModel.getValueAt(popupRow, 0);
		String cmd = event.getActionCommand();
		Object o = event.getSource();

		if (releaseMenuItem == o) {
			myService.send(boundServiceName, "releaseService", releasedTarget.getName());
			return;
		}

		if ("info".equals(cmd)) {
			BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + c.getSimpleName());
		} else if ("install".equals(cmd)) {
			int selectedRow = possibleServices.getSelectedRow();

			ServiceType entry = ((ServiceType) possibleServices.getValueAt(selectedRow, 0));
			Repo repo = myRuntime.getRepo();

			if (!repo.isServiceTypeInstalled(entry.name)) {
				// dependencies needed !!!
				String msg = "<html>This Service has dependencies which are not yet loaded,<br>" + "do you wish to download them now?";
				JOptionPane.setRootFrame(myService.getFrame());
				int result = JOptionPane.showConfirmDialog(myService.getFrame(), msg, "alert", JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.CANCEL_OPTION) {
					return;
				}
				// you say "install", i say "update", repo says "resolve"
				myService.send(boundServiceName, "update", entry.name);
			} else {
				// no unfulfilled dependencies - good to go
				addNewService(entry.name);
			}

		} else if ("start".equals(cmd)) {
			int selectedRow = possibleServices.getSelectedRow();
			ServiceType entry = ((ServiceType) possibleServices.getValueAt(selectedRow, 0));
			addNewService(entry.name);

		} else if ("install all".equals(cmd)) {
			myService.send(boundServiceName, "updateAll");
			/*
			 * } else if ("upgrade".equals(cmd)) { // INSTALL OF A SERVICE //
			 * send to "my" runtime - may be remote
			 * myService.send(myRuntime.getName(), "update", entry.name);
			 */
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

			/*
			 * } else if (cmd.equals(Appender.REMOTE)) { JCheckBoxMenuItem m =
			 * (JCheckBoxMenuItem) ae.getSource(); if (m.isSelected()) {
			 * ConnectDialog dlg = new ConnectDialog(new JFrame(),
			 * "connect to remote logging", "message", this, "127.0.0.1",
			 * "6767"); Logging logging = LoggingFactory.getInstance();
			 * logging.addAppender(Appender.REMOTE, dlg.host.getText(),
			 * dlg.port.getText()); } else { Logging logging =
			 * LoggingFactory.getInstance();
			 * logging.removeAppender(Appender.REMOTE); }
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
	 * FIXME - on repo change (install) - need an event hook
	 */
	@Override
	public void attachGUI() {

		// check to see if there are updates
		subscribe("checkingForUpdates", "checkingForUpdates");

		// results of checkForUpdates
		subscribe("publishUpdates", "publishUpdates", Updates.class);

		subscribe("updatesBegin", "updatesBegin", Updates.class);
		subscribe("updateProgress", "updateProgress", Status.class);
		subscribe("updatesFinished", "updatesFinished", ArrayList.class);
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

	public void checkingForUpdates() {
		// FIXME - if auto update or check on startup - we don't want a silly
		// dialog
		// we just want to be notified if there "is" an update - and whether or
		// not we
		// should apply it

		// FIXME - bypass is auto
		progressDialog.checkingForUpdates();
	}

	@Override
	public void detachGUI() {

		unsubscribe("checkingForUpdates", "checkingForUpdates");

		unsubscribe("publishUpdates", "publishUpdates", Updates.class);

		unsubscribe("updatesBegin", "updatesBegin", Updates.class);
		unsubscribe("updateProgress", "updateProgress", Status.class);
		unsubscribe("updatesFinished", "updatesFinished", ArrayList.class);

	}

	public void failedDependency(String dep) {
		JOptionPane.showMessageDialog(null, "<html>Unable to load Service...<br>" + dep + "</html>", "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Add data to the list model for display
	 */
	public void getCurrentServices() {
		HashMap<String, ServiceInterface> services = Runtime.getRegistry();

		Map<String, ServiceInterface> sortedMap = null;
		sortedMap = new TreeMap<String, ServiceInterface>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceInterface si = Runtime.getService(serviceName);
			currentServicesModel.addElement(si);
			nameToServiceEntry.put(serviceName, si);
		}
	}

	public void getPossibleServices() {
		getPossibleServices(null);
	}

	/**
	 * lame - deprecate - refactor - or better yet make webgui FIXME this should
	 * rarely change .... remove getServiceTypeNames
	 * 
	 * @param serviceTypeNames
	 */
	public void getPossibleServices(final String filter) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// clear data
				for (int i = possibleServicesModel.getRowCount(); i > 0; --i) {
					possibleServicesModel.removeRow(i - 1);
				}

				Category category = serviceData.getCategory(filter);
				HashSet<String> filtered = null;
				if (category != null) {
					filtered = new HashSet<String>();
					ArrayList<String> f = category.serviceTypes;
					for (int i = 0; i < f.size(); ++i) {
						filtered.add(f.get(i));
					}
				}

				// populate with serviceData
				ArrayList<ServiceType> possibleService = serviceData.getServiceTypes();
				for (int i = 0; i < possibleService.size(); ++i) {
					ServiceType serviceType = possibleService.get(i);
					if (filtered == null || filtered.contains(serviceType.name)) {
						if (serviceType.isAvailable()) {
							possibleServicesModel.addRow(new Object[] { serviceType, "" });
						}
					}
				}

				possibleServicesModel.fireTableDataChanged();
				possibleServices.invalidate();
			}
		});
	}

	public void getState(final Runtime runtime) {
		myRuntime = runtime;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// FIXME - change to "all" or "" - null is sloppy - system has
				// to upcast

			}
		});
	}

	@Override
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
		possibleServices.setDefaultRenderer(ServiceType.class, cellRenderer);
		possibleServices.setDefaultRenderer(String.class, cellRenderer);

		possibleServices.addMouseListener(new MouseAdapter() {
			// isPopupTrigger over OSs - use masks
			@Override
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
				ServiceType c = (ServiceType) possibleServicesModel.getValueAt(popupRow, 0);
				releaseMenuItem.setVisible(false);
				infoMenuItem.setVisible(true);
				if (!myRepo.isServiceTypeInstalled(c.name)) {
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
			@Override
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
					releasedTarget = (ServiceInterface) source.getModel().getElementAt(index);
					log.info(String.format("right click on running service %s", releasedTarget.getName()));
					releaseMenuItem.setVisible(true);
					upgradeMenuItem.setVisible(false);
					installMenuItem.setVisible(false);
					startMenuItem.setVisible(false);
					infoMenuItem.setVisible(false);
				}
				popup.show(e.getComponent(), e.getX(), e.getY());

			}

		});

		infoMenuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
		infoMenuItem.setActionCommand("info");
		infoMenuItem.setIcon(Util.getImageIcon("help.png"));
		infoMenuItem.addActionListener(this);
		popup.add(infoMenuItem);

		installMenuItem = new JMenuItem("install");
		installMenuItem.addActionListener(this);
		installMenuItem.setIcon(Util.getImageIcon("install.png"));
		popup.add(installMenuItem);

		startMenuItem = new JMenuItem("start");
		startMenuItem.addActionListener(this);
		startMenuItem.setIcon(Util.getImageIcon("start.png"));
		popup.add(startMenuItem);

		upgradeMenuItem = new JMenuItem("upgrade");
		upgradeMenuItem.addActionListener(this);
		upgradeMenuItem.setIcon(Util.getImageIcon("upgrade.png"));
		popup.add(upgradeMenuItem);

		releaseMenuItem = new JMenuItem("release");
		releaseMenuItem.addActionListener(this);
		releaseMenuItem.setIcon(Util.getScaledIcon(Util.getImage("release.png"), 0.50));
		popup.add(releaseMenuItem);

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

		if (myRuntime != Runtime.getInstance()) {
			log.info("foreign runtime");
		}

		// category toolbar
		ArrayList<Category> cats = serviceData.getAvailableCategories();

		JPanel flowLayout = new JPanel();
		flowLayout.setPreferredSize(new Dimension(300, 160));

		JToolBar toolbar = new JToolBar();
		JButton all = new JButton("all");
		all.addActionListener(filterListener);
		toolbar.add(all);
		int t = 0;
		for (int j = 0; j < cats.size(); ++j) {
			t += 1;
			JButton b = new JButton(cats.get(j).name);
			b.addActionListener(filterListener);
			toolbar.add(b);
			if (t % 8 == 0) {
				flowLayout.add(toolbar);
				toolbar = new JToolBar();
			}
		}

		flowLayout.add(toolbar);

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

		// TODO - get memory total & free - put in Platform? getMemory has to be
		// implemented as a callback
		title = BorderFactory.createTitledBorder(String.format("%s %s", platform.getPlatformId(), platform.getVersion()));
		center.setBorder(title);

		JMenuBar menuBar = new JMenuBar();
		JMenu system = new JMenu("system");
		menuBar.add(system);
		JMenu logging = new JMenu("logging");
		menuBar.add(logging);

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

		display.add(menuBar, BorderLayout.NORTH);
		display.add(center, BorderLayout.CENTER);

		display.add(flowLayout, BorderLayout.SOUTH);

		getPossibleServices();
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

	/**
	 * new Service has been created list it..
	 * 
	 * @param sw
	 * @return
	 */
	public ServiceInterface registered(Service sw) {
		if (!nameToServiceEntry.containsKey(sw.getName())) {
			currentServicesModel.addElement(sw);
			nameToServiceEntry.put(sw.getName(), sw);
		}
		return sw;
	}

	/**
	 * a Service of this Runtime has been released
	 * 
	 * @param sw
	 * @return
	 */
	public ServiceInterface released(Service sw) {
		if (nameToServiceEntry.containsKey(sw.getName())) {
			currentServicesModel.removeElement(nameToServiceEntry.get(sw.getName()));
		} else {
			log.error(sw.getName() + " released event - but could not find in currentServiceModel");
		}
		return sw;
	}

	/**
	 * a restart command will be sent to the appropriate runtime
	 */
	public void restart() {
		send("restart");
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
	 * updatesFinished - finished processing updates. Looking for confirmation
	 * of a restart or a no worky in case of errors
	 * 
	 * @param report
	 */
	public void updatesFinished(ArrayList<ResolveReport> report) {
		progressDialog.addInfo("finished processing updates ");
		progressDialog.finished();
	}

}