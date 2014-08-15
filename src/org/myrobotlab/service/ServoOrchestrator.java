package org.myrobotlab.service;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ListSelectionEvent;

import org.myrobotlab.control.ServoOrchestratorGUI_middlemiddle_panel;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * based on _TemplateService
 * "Clock-Code" modified from "Clock-Service"
 */
/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class ServoOrchestrator extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory
			.getLogger(ServoOrchestrator.class);

	org.myrobotlab.control.ServoOrchestratorGUI sogui_ref;

	// TODO - don't define size (or at least make it bigger)
	// rework, that it could be made bigger
	int sizex = 5;
	int sizey = 5;

	SettingsItemHolder[] settingsitemholder;
	
	Servo[] servos;

	public boolean isClockRunning;
	public int interval = 1;

	public transient ClockThread myClock = null;

	int middleright_shownitem;
	
	int pos1;
	int pos2;
	int pos3;

	public ServoOrchestrator(String n) {
		super(n);
		// intializing variables
		settingsitemholder = new SettingsItemHolder[sizey];
		servos = new Servo[sizey];
		for (int i = 0; i < settingsitemholder.length; i++) {
			SettingsItemHolder sih = new SettingsItemHolder();
			sih.name = "Channel " + (i + 1);
			sih.min = 0;
			sih.max = 180;
			sih.startvalue = (sih.min + sih.max) / 2;
			sih.arduinopos = 0;
			sih.pinpos = 0;
			sih.attached = false;
			settingsitemholder[i] = sih;
		}
		pos1 = 1;
		pos2 = 1;
		pos3 = 000;
	}

	@Override
	public void startService() {
		super.startService();
	}

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public String getDescription() {
		return "organize your Servo-movements";
	}

	public void setsoguireference(
			org.myrobotlab.control.ServoOrchestratorGUI so_ref) {
		sogui_ref = so_ref;
	}

	public void setmiddlemiddlesize() {
		sogui_ref.sizex = sizex;
		sogui_ref.sizey = sizey;
	}
	
	public void set_middleright_arduino_list_items() {
		List<ServiceInterface> services = Runtime.getServices();
		ArrayList<String> arduinolist = new ArrayList<String>();
		for (ServiceInterface service : services) {
			String type = service.getType();
			String typ = type.substring(23);
			if (typ.equals("Arduino")) {
				String name = service.getName();
				arduinolist.add(name);
			}
		}
		String[] arduinoarray = new String[arduinolist.size()+2];
		arduinoarray[0] = "          ";
		arduinoarray[1] = "refresh";
		for (int i = 0; i < arduinolist.size(); i++) {
			arduinoarray[i+2] = arduinolist.get(i);
		}
		sogui_ref.middleright_arduino_list.setListData(arduinoarray);
	}

	public void middleright_update_button() {
		settingsitemholder[middleright_shownitem].name = sogui_ref.middleright_name_textfield
				.getText();
		settingsitemholder[middleright_shownitem].min = Integer
				.parseInt(sogui_ref.middleright_min_textfield.getText());
		settingsitemholder[middleright_shownitem].max = Integer
				.parseInt(sogui_ref.middleright_max_textfield.getText());
		settingsitemholder[middleright_shownitem].startvalue = Integer
				.parseInt(sogui_ref.middleright_startvalue_textfield.getText());
	}
	
	public void middleright_attach_button() {
		if (settingsitemholder[middleright_shownitem].attached) {
			settingsitemholder[middleright_shownitem].arduinopos = sogui_ref.middleright_arduino_list.getSelectedIndex();
			settingsitemholder[middleright_shownitem].pinpos = sogui_ref.middleright_pin_list.getSelectedIndex();
		String arduino = (String) sogui_ref.middleright_arduino_list.getSelectedValue();
		int pin = Integer.parseInt((String) sogui_ref.middleright_pin_list.getSelectedValue());
		servos[middleright_shownitem] = (Servo) Runtime.start("so." + middleright_shownitem, "Servo");
		servos[middleright_shownitem].attach(arduino, pin);
		boolean attach = servos[middleright_shownitem].attach();
		if (attach) {
			sogui_ref.middleright_attach_button.setText("Detach");
			settingsitemholder[middleright_shownitem].attached = true;
		}
		} else {
			boolean detach = servos[middleright_shownitem].detach();
			servos[middleright_shownitem] = null;
			if (detach) {
				sogui_ref.middleright_attach_button.setText("Attach");
				settingsitemholder[middleright_shownitem].attached = false;
			}
		}
	}

	public void bottommiddlerightbottom_button_1() {
		//TODO - add functionality
	}
	
	public void bottommiddlerightbottom_button_2() {
		//TODO - add functionality
	}
	
	public void bottommiddlerightbottom_button_3() {
		//TODO - add functionality
	}
	
	public void bottommiddlerightbottom_button_4() {
		//TODO - add functionality
	}
	
	public void bottommiddlerightbottom_button_5() {
		//TODO - add functionality
	}
	
	public void bottommiddlerightbottom_button_6() {
		play_go_stop();
	}

	public void bottommiddlerightbottom_button_7() {
		play_go_start();
	}
	
	public void bottommiddlerightbottom_button_8() {
		//TODO - add functionality
	}
	
	public void middleright_arduino_list() {
		String selvalue = (String) sogui_ref.middleright_arduino_list.getSelectedValue();
		if (selvalue == null) {
			
		} else if (selvalue.equals("          ")) {
			//1.
		} else if (selvalue.equals("refresh")) {
			//2.
			set_middleright_arduino_list_items();
		} else {
			//3.+
		}
	}

	public void externalcall_loadsettings(int pos) {
		middleright_shownitem = pos;
		sogui_ref.middleright_name_textfield.setText(settingsitemholder[pos].name);
		sogui_ref.middleright_min_textfield.setText(settingsitemholder[pos].min + "");
		sogui_ref.middleright_max_textfield.setText(settingsitemholder[pos].max + "");
		sogui_ref.middleright_startvalue_textfield.setText(settingsitemholder[pos].startvalue
				+ "");
		sogui_ref.middleright_arduino_list.setSelectedIndex(settingsitemholder[middleright_shownitem].arduinopos);
		sogui_ref.middleright_pin_list.setSelectedIndex(settingsitemholder[middleright_shownitem].pinpos);
		if (!settingsitemholder[middleright_shownitem].attached) {
			sogui_ref.middleright_attach_button.setText("Attach");
		} else {
			sogui_ref.middleright_attach_button.setText("Detach");
		}
	}
	
	public void play_go_start() {
		sogui_ref.bottommiddlerightbottom_button_6.setEnabled(true);
		sogui_ref.bottommiddlerightbottom_button_7.setEnabled(false);
		startClock();
	}
	
	public void play_go_stop() {
		sogui_ref.bottommiddlerightbottom_button_6.setEnabled(false);
		sogui_ref.bottommiddlerightbottom_button_7.setEnabled(true);
		stopClock();
	}

	public void play_go_f1() {
		//TODO - add functionality
	}

	public void play_go_b1() {
		//TODO - add functionality
	}

	public void play_go_fa() {
		//TODO - add functionality
	}

	public void play_go_ba() {
		//TODO - add functionality
	}

	public void play_play_1_1() {
		pos1++;
		sogui_ref.bottommiddlerighttop_textfield_1.setText(pos1 + "");
		if (pos1 > sizex) {
			play_go_stop();
		}
		play_playreally(pos1);
	}
	
	public void play_play_2_1() {
		pos2++;
		if (pos2 > 4) {
			play_play_1_1();
			pos2 -= 4;
		}
		sogui_ref.bottommiddlerighttop_textfield_2.setText(pos2 + "");
	}
	
	public void play_play_3_1() {
		pos3++;
		if (pos3 > 999) {
			play_play_2_1();
			pos3 -= 999;
		}
		sogui_ref.bottommiddlerighttop_textfield_3.setText(pos3 + "");
	}
	
	public void play_playreally(int pos) {
		sogui_ref.middlemiddle_ref.prep[sogui_ref.middlemiddle_ref.getRandomDragAndDropPanels()[0].length+pos-2].setBackground(Color.green);
		sogui_ref.middlemiddle_ref.prep[sogui_ref.middlemiddle_ref.getRandomDragAndDropPanels()[0].length+pos-1].setBackground(Color.red);
		sogui_ref.middlemiddle_ref.relayout();
		if (pos >= sizex) {
			play_searchblocks(pos);
		}
	}
	
	public void play_searchblocks(int pos) {
		for (int i = 0; i < sizey; i++) {
			ServoOrchestratorGUI_middlemiddle_panel panels11 = sogui_ref.middlemiddle_ref.getRandomDragAndDropPanels()[pos][i];
			if (panels11 != null) {
				play_playblock(i, panels11);
			}
		}
	}
	
	public void play_playblock(int channel, ServoOrchestratorGUI_middlemiddle_panel block) {
		switch (block.type) {
		case "timesection":
			break;
		case "channel":
			break;
		case "servo":
			if (servos[channel] != null) {
				int movetopos = Integer.parseInt(block.servo_goal.getText());
				servos[channel].moveTo(movetopos);
			}
			break;
		}
	}

	public class SettingsItemHolder {

		String name;
		int min;
		int max;
		int startvalue;
		int arduinopos;
		int pinpos;
		boolean attached;
	}

	public class ClockThread implements Runnable {
		public Thread thread = null;

		ClockThread() {
			thread = new Thread(this, getName() + "_ticking_thread");
			thread.start();
		}

		public void run() {
			try {
				while (isClockRunning == true) {
					play_play_3_1();
					Thread.sleep(interval);
				}
			} catch (InterruptedException e) {
				isClockRunning = false;
			}
		}
	}

	public void startClock() {
		if (myClock == null) {
			isClockRunning = true;
			myClock = new ClockThread();
		}
	}

	public void stopClock() {
		if (myClock != null) {
			isClockRunning = false;
			myClock.thread.interrupt();
			myClock.thread = null;
			myClock = null;
		}
		isClockRunning = false;
	}

	public static void main(String[] args) throws InterruptedException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {

			Runtime.start("gui", "GUIService");
			Runtime.start("servoorchestrator", "ServoOrchestrator");

		} catch (Exception e) {
			Logging.logException(e);
		}

	}
}

