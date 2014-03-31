package org.myrobotlab.dynamicGUI;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.HashMap;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class Desktop extends JFrame implements MouseListener,
		MouseMotionListener, WindowListener, ActionListener {
	public JPopupMenu popup1 = new JPopupMenu();
	public JPopupMenu popup2 = new JPopupMenu();
	public JPopupMenu popup3 = new JPopupMenu();
	public JPopupMenu popup4 = new JPopupMenu();
	private int x, y, x1, y1, x2, y2;
	private boolean drag = false;
	private Component comp;
	private JCheckBoxMenuItem cbmni = new JCheckBoxMenuItem("Lockdown", false);
	private HashMap<Component,Container> componentMap=new HashMap<Component,Container>();
	private int compx;
	private int compy;

	public Desktop() {
		super("GUIDynamic");
		JMenuBar jm=new JMenuBar();
		JMenu menu1=new JMenu("Menu");
		jm.add(menu1);
		menu1.add(cbmni);
		this.setJMenuBar(jm);
		this.addWindowListener(this);
		imageicon();
		this.pack();
		this.setSize(640, 480);
		Dimension d67 = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d68 = this.getSize();
		this.setLocation((int) (d67.width / 2 - d68.width / 2),
				(int) (d67.height / 2 - d68.height / 2));
		cbmni.addActionListener(this);
		JMenuItem jmi11 = new JMenuItem("Add CommandField");
		jmi11.addActionListener(this);
		JMenuItem jmi12 = new JMenuItem("Add ConsoleArea");
		jmi12.addActionListener(this);
		JMenuItem jmi13 = new JMenuItem("Add ScriptArea");
		jmi13.addActionListener(this);
		JMenuItem jmi14 = new JMenuItem("Help");
		jmi14.addActionListener(this);
		popup1.add(jmi14);
		popup1.addSeparator();
		popup1.add(jmi11);
		popup1.add(jmi12);
		popup1.add(jmi13);
		popup1.addSeparator();
		//popup1.add(cbmni);

		JMenuItem jmi21 = new JMenuItem("Delete");
		jmi21.addActionListener(this);
		JMenuItem jmi22 = new JMenuItem("Help");
		jmi22.addActionListener(this);
		JMenuItem jmi23 = new JMenuItem("Change To Button");
		jmi23.addActionListener(this);
		JMenuItem jmi24 = new JMenuItem("Transplant");
		jmi24.addActionListener(this);
		popup2.add(jmi22);
		popup2.addSeparator();
		popup2.add(jmi23);
		popup2.addSeparator();
		popup2.add(jmi21);
		popup2.add(jmi24);

		JMenuItem jmi31 = new JMenuItem("Delete");
		jmi31.addActionListener(this);
		JMenuItem jmi32 = new JMenuItem("Help");
		jmi32.addActionListener(this);
		JMenuItem jmi33 = new JMenuItem("Change Back");
		jmi33.addActionListener(this);
		JMenuItem jmi34 = new JMenuItem("Transplant");
		jmi34.addActionListener(this);
		popup3.add(jmi32);
		popup3.addSeparator();
		popup3.add(jmi33);
		popup3.addSeparator();
		popup3.add(jmi31);
		popup3.add(jmi34);
		JMenuItem jmi41 = new JMenuItem("Delete");
		jmi41.addActionListener(this);
		JMenuItem jmi42 = new JMenuItem("Help");
		jmi42.addActionListener(this);
		JMenuItem jmi43 = new JMenuItem("Hook");
		jmi43.addActionListener(this);
		popup4.add(jmi42);
		popup4.addSeparator();
		popup4.add(jmi41);
		popup4.add(jmi43);
		this.getContentPane().setLayout(null);
	}

	private void imageicon() {
		URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		this.setIconImage(img);
	}

	private void listenerremove(Container con) {
		if (con == null)
			return;
		Component[] com = con.getComponents();
		for (int r2 = 0; r2 < com.length; r2++) {
			if (com[r2] instanceof Container) {
				listenerremove((Container) com[r2]);
			}
			com[r2].removeMouseListener(this);
			com[r2].removeMouseMotionListener(this);
		}
	}

	private void listeneradd(Container con) {
		if (con == null)
			return;
		Component[] com = con.getComponents();
		for (int r2 = 0; r2 < com.length; r2++) {
			if (com[r2] instanceof Container) {
				listeneradd((Container) com[r2]);
			}
			com[r2].addMouseListener(this);
			com[r2].addMouseMotionListener(this);
		}
	}

	public void facelift(Container con) {
//		con.setLayout(null);
		listeneradd(con);
		for (Component c : con.getComponents()) {
			if (c instanceof Container)
				facelift((Container) c);
			else
				facelift(c);
		}
		con.validate();
		con.repaint();
	}

	public void transplant(Component con){
		Rectangle b=con.getBounds();
		Container par=con.getParent();
		componentMap.put(con, par);
		par.remove(con);
		par.validate();
		con.setBounds(b);
		con.setLocation(0,0);
		con.validate();
		con.repaint();
		this.getContentPane().add(con);
		this.getContentPane().validate();
		this.getContentPane().repaint();
	}
	
	public void facelift(Component con) {
		con.addMouseListener(this);
		con.addMouseMotionListener(this);
		con.validate();
		con.repaint();
		listeneradd(con.getParent());
	}

	public void facelift(JFrame con) {
		facelift(con.getContentPane());
		con.setVisible(false);
	}

	public void facelift(Frame con) {
		Component[] com = con.getComponents();
		for (int r2 = 0; r2 < com.length; r2++) {
			facelift(com[r2]);
		}
		con.hide();
	}

	private void mouseadd1(Component te) {
		if (te instanceof Container) {
			for (int r = 0; r < ((Container) te).getComponentCount(); r++) {
				mouseadd1(((Container) te).getComponent(r));
			}
		}
		te.addMouseListener(this);
		te.addMouseMotionListener(this);
	}

	private void mouseadd(Component te, int x, int y, int x1, int y1) {
		mouseadd1(te);
		te.setBounds(x, y, x1, y1);
	}

	private void mouserem1(Component te) {
		if (te instanceof Container) {
			for (int r = 0; r < ((Container) te).getComponentCount(); r++) {
				mouserem1(((Container) te).getComponent(r));
			}
		}
		te.removeMouseListener(this);
		te.removeMouseMotionListener(this);
	}

	private void mouserem(Component te) {
		mouserem1(te);
		Container pa=te.getParent();
		pa.remove(te);
		
//		te.validate();
//		te.repaint();
		pa.getParent().validate();
		pa.getParent().repaint();
	}

	// public void hook(Component com) {
	// // int cmdcount=new Random().nextInt(9000)+1000;
	// String title = "comp" + compcount;
	// compcount++;
	// for (int r = 0; r < panel.getComponentCount(); r++) {
	// if (panel.getComponent(r) == com) {
	// System.out.println(("Component " + title + " hooked."));
	// String exec = title + "=main.desktop.panel.getComponent(" + r
	// + ");\n";
	// doConsole(exec);
	// break;
	// }
	// }
	// }

	public void help() {
		String exec = "";
		exec += "Desktop help:\n";
		exec += "Right click on the panel to bring up the menu.\n";
		exec += "Double left click on a component to activate it.\n";
		exec += "Double right click on a component to open its menu.\n";
		exec += "Left click and drag on a component to move it.\n";
		exec += "Right click and drag on a component to resize it.\n\n";
		exec += "Adding Components:\n";
		exec += "Add CommandField      - Type a single command in here.\n";
		exec += "Add ConsoleArea       - Type Java code in here.\n";
		exec += "Add ScriptArea        - Type many commands in here.\n\n";
		exec += "Other:\n";
		exec += "Change To Button      - Folds component into a button.\n";
		exec += "Change Back           - Changes component back.\n";
		exec += "Hook                  - Hooks the component into a variable.\n";
		exec += "Lockdown              - When locked, components can't\n";
		exec += "                        be resized or moved.\n";
		final String exec1 = exec;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int selection = JOptionPane.showConfirmDialog(null, exec1,
						"Selection : ", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.INFORMATION_MESSAGE);
				System.out.println("I be written"
						+ " after you close, the JOptionPane");
				if (selection == JOptionPane.OK_OPTION) {
					// Code to use when OK is PRESSED.
					System.out.println("Selected Option is OK : " + selection);
				} else if (selection == JOptionPane.CANCEL_OPTION) {
					// Code to use when CANCEL is PRESSED.
					System.out.println("Selected Option Is CANCEL : "
							+ selection);
				}
			}
		});
	}

	public void lockdown(boolean b) {
		cbmni.setState(b);
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
		if (drag && !cbmni.getState()) {
			x2 = e.getX()-x1/2 ;
			y2 = e.getY()-y1 /2 ;
			if(x2==0&&y2==0)return;
			Rectangle ptemp = comp.getBounds();
			ptemp.translate(x2, y2);
			System.out.println(x1+","+y1+"\t"+x2+","+y2);
			if (e.isMetaDown()) {
				if (ptemp.width - x2 > 20 && ptemp.height - y2 > 20) {
					comp.setBounds((int) ptemp.x - x2,
							(int) ptemp.y - y2, (int) ptemp.width + x2,
							(int) ptemp.height + y2);
					comp.validate();
					x1 = e.getX()*2;
					y1 = e.getY()*2;
				}

			} else {
				comp.setLocation((int)(comp.getLocation().x+x2),(int)(comp.getLocation().y+y2));	
		         x1=e.getX()*2+x2;
		         y1=e.getY()*2+y2;
			}

		}
	}

	public void mouseReleased(MouseEvent e) {
		drag = false;
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		x1 = e.getX()*2;
		y1 =e.getY()*2;
		compx=comp.getX();
		compy=comp.getY();
		drag = true;
		if (!e.isMetaDown() && e.getClickCount() > 1) {
			doubleclick(comp);
		}
		x = e.getX();
		y = e.getY();
//		if (e.isMetaDown() && e.getSource() == panel) {
//			popup1.show(e.getComponent(), e.getX(), e.getY());
//		}
		if (e.isMetaDown()  && e.getClickCount() > 1) {
				popup3.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
		if (!drag) {
			comp = (Component) e.getSource();
		}
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
//			if (e.getActionCommand().equals("Change To Button")) {
//				DesktopButton b = new DesktopButton();
//				b.setMargin(new Insets(0, 0, 0, 0));
//				b.setText(new SimpleInput().getString("Button Label?"));
//				mouseadd(b, 0, 0, 0, 0);
//				b.setBounds(compparent.getBounds());
//				if (compparent instanceof ScriptArea) {
//					b.type = "ScriptArea";
//					b.action = ((ScriptArea) compparent).getText();
//				}
//				if (compparent instanceof ConsoleArea) {
//					b.type = "ConsoleArea";
//					b.action = ((ConsoleArea) compparent).getText();
//				}
//				if (compparent instanceof CommandField) {
//					b.type = "CommandField";
//					b.action = ((CommandField) compparent).getText();
//				}
//				mouserem(compparent);
//				compparent = b;
//				comp = b;
//			}
//			if (e.getActionCommand().equals("Change Back")) {
//				Component b = null;
//				if (((DesktopButton) compparent).type.equals("ScriptArea")) {
//					b = (Component) new ScriptArea();
//					((ScriptArea) b)
//							.setText(((DesktopButton) compparent).action);
//				}
//				if (((DesktopButton) compparent).type.equals("ConsoleArea")) {
//					b = (Component) new ConsoleArea();
//					((ConsoleArea) b)
//							.setText(((DesktopButton) compparent).action);
//				}
//				if (((DesktopButton) compparent).type.equals("CommandField")) {
//					b = (Component) new CommandField();
//					((CommandField) b)
//							.setText(((DesktopButton) compparent).action);
//				}
//				mouseadd(b, 0, 0, 0, 0);
//				b.setBounds(compparent.getBounds());
//				mouserem(compparent);
//				compparent = b;
//				comp = b;
//			}
			if (e.getActionCommand().equals("Add CommandField")) {
				mouseadd(new CommandField(), x, y, 50, 25);
			}
			if (e.getActionCommand().equals("Add ConsoleArea")) {
				mouseadd(new ConsoleArea(), x, y, 100, 100);
			}
			if (e.getActionCommand().equals("Add ScriptArea")) {
				mouseadd(new ScriptArea(), x, y, 100, 100);
			}
			if (e.getActionCommand().equals("Delete")) {
				mouserem(comp);
			}
			
			if (e.getActionCommand().equals("Change Back")) {
				untransplant(comp);
			}

			
			if (e.getActionCommand().equals("Transplant")) {
				transplant(comp);
			}

			// if (e.getActionCommand().equals("Hook")) {
			// hook(compparent);
			// }
			if (e.getActionCommand().equals("Help")) {
				help();
			}
		}
	}

	// public void doCommand(String com){
	// main.action(com);
	// }
	// public void doConsole(String con){
	// try{main.execute(con);}
	// catch(Exception e){System.out.println(e);}
	// }
	// public void doScript(String scr){
	// new Commands(main,"script "+scr);
	// }

	private void untransplant(Component con) {
		Container newpar=componentMap.get(con);
		if(newpar==null)return;
		this.getContentPane().remove(con);
		newpar.add(con);
		con.validate();
		con.repaint();
		this.getContentPane().validate();
		this.getContentPane().repaint();
		newpar.validate();
		newpar.repaint();
		newpar.getParent().validate();
		newpar.getParent().repaint();
	}

	public void doubleclick(Object o) {
		// if (o instanceof DesktopButton){
		// DesktopButton b=(DesktopButton)o;
		// new DesktopCommandClicked(b);
		// if (b.type.equals("CommandField"))doCommand(b.action);
		// if (b.type.equals("ConsoleArea"))doConsole(b.action);
		// if (b.type.equals("ScriptArea"))doScript(b.action);
		// }
		// if (o instanceof CommandField){
		// new DesktopCommandClicked((CommandField)o);
		// doCommand(((CommandField)o).getText());
		// }
		// if (o instanceof ConsoleArea){
		// new DesktopCommandClicked(((ConsoleArea)o).getTextArea());
		// doConsole(((ConsoleArea)o).getText());
		// }
		// if (o instanceof ScriptArea){
		// new DesktopCommandClicked(((ScriptArea)o).getTextArea());
		// doScript(((ScriptArea)o).getText());
		// }
	}

}
