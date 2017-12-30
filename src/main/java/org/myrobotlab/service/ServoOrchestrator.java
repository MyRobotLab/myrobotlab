package org.myrobotlab.service;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.swing.widget.ServoOrchestratorGUI_middlemiddle_panel;
import org.slf4j.Logger;

/**
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class ServoOrchestrator extends Service {

  public class ClockThread implements Runnable {
    public Thread thread = null;

    ClockThread() {
      thread = new Thread(this, getName() + "_ticking_thread");
      thread.start();
    }

    @Override
    public void run() {
      try {
        while (isClockRunning == true) {
          play_play_3_1();
          Thread.sleep(interval);
        }
      } catch (InterruptedException e) {
        Logging.logError(e);
        isClockRunning = false;
      }
    }
  }

  public class SettingsItemHolder {

    public String name;
    public int min;
    public int max;
    public int startvalue;
    public int arduinopos;
    public int pinpos;
    public boolean attached;
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(ServoOrchestrator.class);
  transient org.myrobotlab.swing.ServoOrchestratorGui sogui_ref;

  int sizex = 5;

  int sizey = 5;

  transient SettingsItemHolder[] settingsitemholder;
  Servo[] servos;

  public boolean isClockRunning;

  public int interval = 1;

  public transient ClockThread myClock = null;

  int middleright_shownitem;
  boolean click_play = true;
  int pos1;

  int pos2;

  int pos3;

  public static void main(String[] args) throws InterruptedException {

    LoggingFactory.init(Level.INFO);
    try {

      Runtime.start("gui", "SwingGui");
      Runtime.start("servoorchestrator", "ServoOrchestrator");

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public ServoOrchestrator(String n) {
    super(n);
    // intializing variables
    settingsitemholder = new SettingsItemHolder[sizey];
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
    servos = new Servo[sizey];
    pos1 = 1;
    pos2 = 1;
    pos3 = 000;
  }

  public void bottommiddlerightbottom_button_1() {
    play_go_ba();
  }

  public void bottommiddlerightbottom_button_2() {
    play_go_fa();
  }

  public void bottommiddlerightbottom_button_3() {
    play_go_b1();
  }

  public void bottommiddlerightbottom_button_4() {
    play_go_f1();
  }

  public void bottommiddlerightbottom_button_5() {
    // TODO - add functionality
  }

  public void bottommiddlerightbottom_button_6() {
    play_go_stop();
  }

  public void bottommiddlerightbottom_button_7() {
    play_go_start();
  }

  public void bottommiddlerightbottom_button_8() {
    // TODO - add functionality
  }

  public void bottommiddlerighttop_update_button() {
    pos1 = Integer.parseInt(sogui_ref.bottommiddlerighttop_textfield_1.getText());
    pos2 = Integer.parseInt(sogui_ref.bottommiddlerighttop_textfield_2.getText());
    pos3 = Integer.parseInt(sogui_ref.bottommiddlerighttop_textfield_3.getText());
    play_updatetime(true, true, true);
    play_updatepanels(pos1);
  }

  public void bottomright_click_checkbox() {
    click_play = sogui_ref.bottomright_click_checkbox.isSelected();
  }

  public void externalcall_loadsettings(int pos) {
    middleright_shownitem = pos;
    sogui_ref.middleright_name_textfield.setText(settingsitemholder[pos].name);
    sogui_ref.middleright_min_textfield.setText(settingsitemholder[pos].min + "");
    sogui_ref.middleright_max_textfield.setText(settingsitemholder[pos].max + "");
    sogui_ref.middleright_startvalue_textfield.setText(settingsitemholder[pos].startvalue + "");
    sogui_ref.middleright_arduino_list.setSelectedIndex(settingsitemholder[middleright_shownitem].arduinopos);
    sogui_ref.middleright_pin_list.setSelectedIndex(settingsitemholder[middleright_shownitem].pinpos);
    if (!settingsitemholder[middleright_shownitem].attached) {
      sogui_ref.middleright_attach_button.setText("Attach");
    } else {
      sogui_ref.middleright_attach_button.setText("Detach");
    }
  }

  public void externalcall_servopanelchangeinfo(int x, int y) {
    sogui_ref.middlemiddle_ref.panels[x][y].servo_min.setText(settingsitemholder[y].min + "");
    sogui_ref.middlemiddle_ref.panels[x][y].servo_max.setText(settingsitemholder[y].max + "");
  }

  public void externalcall_servopanelsettostartpos(int x, int y, boolean withgoal) {
    sogui_ref.middlemiddle_ref.panels[x][y].servo_start.setText(settingsitemholder[y].startvalue + "");
    if (withgoal) {
      sogui_ref.middlemiddle_ref.panels[x][y].servo_goal.setText(settingsitemholder[y].startvalue + "");
    }
  }

  public void middleleft_channeladd_button() {
    sizey++;
    sogui_ref.sizey = sizey;
    sogui_ref.middlemiddle_ref.externallcall_refreshsize();
    refreshsize();
  }

  public void middleleft_channelremove_button() {
    sizey--;
    sogui_ref.sizey = sizey;
    sogui_ref.middlemiddle_ref.externallcall_refreshsize();
    refreshsize();
  }

  public void middleleft_timeunitadd_button() {
    sizex++;
    sogui_ref.sizex = sizex;
    sogui_ref.middlemiddle_ref.externallcall_refreshsize();
  }

  public void middleleft_timeunitremove_button() {
    sizex--;
    sogui_ref.sizex = sizex;
    sogui_ref.middlemiddle_ref.externallcall_refreshsize();
  }

  public void middleright_arduino_list() {
    String selvalue = (String) sogui_ref.middleright_arduino_list.getSelectedValue();
    if (selvalue == null) {

    } else if (selvalue.equals("          ")) {
      // 1.
    } else if (selvalue.equals("refresh")) {
      // 2.
      set_middleright_arduino_list_items();
    } else {
      // 3.+
    }
  }

  public void middleright_attach_button() throws Exception {
    if (!settingsitemholder[middleright_shownitem].attached) {
      settingsitemholder[middleright_shownitem].arduinopos = sogui_ref.middleright_arduino_list.getSelectedIndex();
      settingsitemholder[middleright_shownitem].pinpos = sogui_ref.middleright_pin_list.getSelectedIndex();
      String arduinoName = (String) sogui_ref.middleright_arduino_list.getSelectedValue();
      Arduino arduino = (Arduino)Runtime.getService(arduinoName);
      int pin = Integer.parseInt((String) sogui_ref.middleright_pin_list.getSelectedValue());
      int min = settingsitemholder[middleright_shownitem].min;
      int max = settingsitemholder[middleright_shownitem].max;
      servos[middleright_shownitem] = (Servo) Runtime.start("so." + middleright_shownitem, "Servo");
      servos[middleright_shownitem].setMinMax(min, max);
      arduino.servoAttachPin(servos[middleright_shownitem], pin);
      servos[middleright_shownitem].attach();
      sogui_ref.middleright_attach_button.setText("Detach");
      settingsitemholder[middleright_shownitem].attached = true;
    } else {
      servos[middleright_shownitem].detach();
      servos[middleright_shownitem] = null;
      sogui_ref.middleright_attach_button.setText("Attach");
      settingsitemholder[middleright_shownitem].attached = false;
    }
  }

  public void middleright_update_button() {
    settingsitemholder[middleright_shownitem].name = sogui_ref.middleright_name_textfield.getText();
    settingsitemholder[middleright_shownitem].min = Integer.parseInt(sogui_ref.middleright_min_textfield.getText());
    settingsitemholder[middleright_shownitem].max = Integer.parseInt(sogui_ref.middleright_max_textfield.getText());
    settingsitemholder[middleright_shownitem].startvalue = Integer.parseInt(sogui_ref.middleright_startvalue_textfield.getText());

    sogui_ref.middlemiddle_ref.prep[middleright_shownitem].channel_name.setText(settingsitemholder[middleright_shownitem].name);
    if (settingsitemholder[middleright_shownitem].attached) {
      int min = settingsitemholder[middleright_shownitem].min;
      int max = settingsitemholder[middleright_shownitem].max;
      servos[middleright_shownitem].setMinMax(min, max);
    }
    for (int i = 0; i < sogui_ref.middlemiddle_ref.panels.length; i++) {
      if (sogui_ref.middlemiddle_ref.panels[i][middleright_shownitem] != null) {
        sogui_ref.middlemiddle_ref.panels[i][middleright_shownitem].servo_min.setText(settingsitemholder[middleright_shownitem].min + "");
        sogui_ref.middlemiddle_ref.panels[i][middleright_shownitem].servo_max.setText(settingsitemholder[middleright_shownitem].max + "");
      }
    }
    // TODO - change the "startvalue"
  }

  public void play_checktime() {
    if (pos1 > sogui_ref.middlemiddle_ref.panels[0].length) {
      pos1 = sogui_ref.middlemiddle_ref.panels[0].length;
    } else if (pos1 < 1) {
      pos1 = 1;
    }
    if (pos2 > 4) {
      pos2 = 4;
    } else if (pos2 < 1) {
      pos2 = 1;
    }
    if (pos3 > 999) {
      pos3 = 999;
    } else if (pos3 < 0) {
      pos3 = 0;
    }
  }

  public void play_go_b1() {
    pos1--;
    play_updatetime(true, false, false);
    play_updatepanels(pos1);
  }

  public void play_go_ba() {
    pos1 = 1;
    pos2 = 1;
    pos3 = 0;
    play_updatetime(true, true, true);
    play_updatepanels(pos1);
  }

  public void play_go_f1() {
    pos1++;
    play_updatetime(true, false, false);
    play_updatepanels(pos1);
  }

  public void play_go_fa() {
    pos1 = sogui_ref.middlemiddle_ref.panels[0].length;
    pos2 = 4;
    pos3 = 999;
    play_updatetime(true, true, true);
    play_updatepanels(pos1);
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

  public void play_play_1_1() {
    pos1++;
    if (pos1 > sizex) {
      play_go_stop();
      pos2 = 4;
      pos3 = 999;
    }
    play_updatetime(true, false, false);
    play_playreally(pos1);
  }

  public void play_play_2_1() {
    pos2++;
    if (pos2 > 4) {
      pos2 -= 4;
      play_play_1_1();
    }
    play_updatetime(false, true, false);
  }

  public void play_play_3_1() {

    // first block
    if (pos1 == 1 && pos2 == 1 && pos3 == 0) {
      play_playreally(pos1);
    }

    pos3++;
    if (pos3 > 999) {
      pos3 -= 999;
      play_play_2_1();
    }
    play_updatetime(false, false, true);
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

  public void play_playclick() {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResourceAsStream("/resource/ServoOrchestrator/click.wav"));
      Clip clip = AudioSystem.getClip();
      clip.open(audioInputStream);
      clip.start();
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public void play_playreally(int pos) {
    play_updatepanels(pos);
    if (click_play) {
      play_playclick();
    }
    if (pos <= sizex) {
      play_searchblocks(pos);
    }
  }

  public void play_searchblocks(int pos) {
    for (int i = 0; i < sizey; i++) {
      ServoOrchestratorGUI_middlemiddle_panel panels11 = sogui_ref.middlemiddle_ref.panels[pos - 1][i];
      if (panels11 != null) {
        play_playblock(i, panels11);
      }
    }
  }

  public void play_updatepanels(int pos) {
    for (int i = 0; i < sogui_ref.middlemiddle_ref.panels[0].length; i++) {
      sogui_ref.middlemiddle_ref.prep[sogui_ref.middlemiddle_ref.panels[0].length + i].setBackground(Color.green);
    }
    sogui_ref.middlemiddle_ref.prep[sogui_ref.middlemiddle_ref.panels[0].length + pos - 1].setBackground(Color.red);
    sogui_ref.middlemiddle_ref.relayout();
  }

  public void play_updatetime(boolean t1, boolean t2, boolean t3) {
    play_checktime();
    if (t1) {
      sogui_ref.bottommiddlerighttop_textfield_1.setText(pos1 + "");
    }
    if (t2) {
      sogui_ref.bottommiddlerighttop_textfield_2.setText(pos2 + "");
    }
    if (t3) {
      sogui_ref.bottommiddlerighttop_textfield_3.setText(pos3 + "");
    }
  }

  public void refreshsize() {
    SettingsItemHolder[] settingsitemholderold = new SettingsItemHolder[sizey];
    settingsitemholderold = settingsitemholder.clone();

    Servo[] servosold = new Servo[sizey];

    settingsitemholder = new SettingsItemHolder[sizey];
    for (int i = 0; i < settingsitemholder.length; i++) {
      if (i >= settingsitemholderold.length) {
        SettingsItemHolder sih = new SettingsItemHolder();
        sih.name = "Channel " + (i + 1);
        sih.min = 0;
        sih.max = 180;
        sih.startvalue = (sih.min + sih.max) / 2;
        sih.arduinopos = 0;
        sih.pinpos = 0;
        sih.attached = false;
        settingsitemholder[i] = sih;
        continue;
      }
      settingsitemholder[i] = settingsitemholderold[i];
    }

    servos = new Servo[sizey];
    for (int i = 0; i < servos.length; i++) {
      if (i >= servosold.length) {
        continue;
      }
      servos[i] = servosold[i];
    }
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
    String[] arduinoarray = new String[arduinolist.size() + 2];
    arduinoarray[0] = "          ";
    arduinoarray[1] = "refresh";
    for (int i = 0; i < arduinolist.size(); i++) {
      arduinoarray[i + 2] = arduinolist.get(i);
    }
    sogui_ref.middleright_arduino_list.setListData(arduinoarray);
  }

  public void setmiddlemiddlesize() {
    sogui_ref.sizex = sizex;
    sogui_ref.sizey = sizey;
  }

  public void setsoguireference(org.myrobotlab.swing.ServoOrchestratorGui so_ref) {
    sogui_ref = so_ref;
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

  public void top_addservo_button() {
    sogui_ref.middlemiddle_ref.externalcall_addPanel();
  }

  public void top_load_button() {
    try {
      BufferedReader br = new BufferedReader(new FileReader("scratchconfig.txt"));
      int counter = 0;
      int type = 0;
      String line;
      while ((line = br.readLine()) != null) {
        if (line.equals("#size")) {
          counter = 0;
          type = 1;
        } else if (line.equals("#sih")) {
          counter = 0;
          type = 2;
        } else if (line.equals("#prep")) {
          counter = 0;
          type = 3;
        } else if (line.equals("#panels")) {
          counter = 0;
          type = 4;
        } else if (type == 1) {
          int size = Integer.parseInt(line);
          if (counter == 0) {
            sizex = size;
            sogui_ref.sizex = sizex;
            sogui_ref.middlemiddle_ref.externallcall_refreshsize();
          } else if (counter == 1) {
            sizey = size;
            sogui_ref.sizey = sizey;
            sogui_ref.middlemiddle_ref.externallcall_refreshsize();
            refreshsize();
          } else {
            sogui_ref.middlemiddle_ref.panel_counter = size;
          }
          counter++;
        } else if (type == 2) {
          String[] linesplit = line.split("~");
          settingsitemholder[counter].name = linesplit[0];
          settingsitemholder[counter].min = Integer.parseInt(linesplit[1]);
          settingsitemholder[counter].max = Integer.parseInt(linesplit[2]);
          settingsitemholder[counter].startvalue = Integer.parseInt(linesplit[3]);
          settingsitemholder[counter].arduinopos = Integer.parseInt(linesplit[4]);
          settingsitemholder[counter].pinpos = Integer.parseInt(linesplit[5]);
          settingsitemholder[counter].attached = Boolean.parseBoolean(linesplit[6]);
          counter++;
        } else if (type == 3) {
          String[] linesplit = line.split("~");
          sogui_ref.middlemiddle_ref.prep[counter] = new ServoOrchestratorGUI_middlemiddle_panel(linesplit);
          if (linesplit[0].equals("channel")) {
            final int counterf = counter;
            sogui_ref.middlemiddle_ref.prep[counter].channel_settings.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent ae) {
                externalcall_loadsettings(counterf);
              }
            });
          } else if (linesplit[0].equals("timesection")) {
            sogui_ref.middlemiddle_ref.prep[counter].timesection_headline.setText("TIMEUNIT " + (counter - sizey + 1));
          }
          sogui_ref.middlemiddle_ref.relayout();
          counter++;
        } else if (type == 4) {
          if (line.equals("null")) {
            sogui_ref.middlemiddle_ref.panels[counter / sizey][counter % sizey] = null;
          } else if (sogui_ref.middlemiddle_ref.panels[counter / sizey][counter % sizey] == null) {
            String[] linesplit = line.split("~");
            sogui_ref.middlemiddle_ref.panels[counter / sizey][counter % sizey] = new ServoOrchestratorGUI_middlemiddle_panel(linesplit);
            final int counterf = counter;
            sogui_ref.middlemiddle_ref.panels[counter / sizey][counter % sizey].servo_goal.getDocument().addDocumentListener(new DocumentListener() {
              public void adjust() {
                int i1 = counterf / sizey;
                int i2 = counterf % sizey;
                int searchpos = i1 + 1;
                while (searchpos < sogui_ref.middlemiddle_ref.panels.length) {
                  if (sogui_ref.middlemiddle_ref.panels[searchpos][i2] == null) {
                    searchpos++;
                  } else {
                    sogui_ref.middlemiddle_ref.panels[searchpos][i2].servo_start
                        .setText(sogui_ref.middlemiddle_ref.panels[counterf / sizey][counterf % sizey].servo_goal.getText() + "");
                    break;
                  }
                }
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                adjust();

              }

              @Override
              public void insertUpdate(DocumentEvent e) {
                adjust();

              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                adjust();

              }
            });
          }
          sogui_ref.middlemiddle_ref.relayout();
          counter++;
        }
      }
      br.close();
    } catch (IOException e) {
      Logging.logError(e);
    }
  }

  public void top_save_button() {
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter("scratchconfig.txt"));
      bw.write("#size");
      bw.newLine();
      bw.write(sizex + "");
      bw.newLine();
      bw.write(sizey + "");
      bw.newLine();
      bw.write(sogui_ref.middlemiddle_ref.panel_counter + "");
      bw.newLine();
      bw.write("#sih");
      bw.newLine();
      for (SettingsItemHolder sih : settingsitemholder) {
        bw.write(sih.name + "~" + sih.min + "~" + sih.max + "~" + sih.startvalue + "~" + sih.arduinopos + "~" + sih.pinpos + "~" + sih.attached);
        bw.newLine();
      }
      bw.write("#prep");
      bw.newLine();
      for (ServoOrchestratorGUI_middlemiddle_panel p : sogui_ref.middlemiddle_ref.prep) {
        bw.write(p.type + "~" + p.id + "~" + p.channel_name.getText() + "~" + p.servo_start.getText() + "~" + p.servo_channelid.getText() + "~" + p.servo_goal.getText() + "~"
            + p.servo_min.getText() + "~" + p.servo_max.getText());
        bw.newLine();
      }
      bw.write("#panels");
      bw.newLine();
      for (ServoOrchestratorGUI_middlemiddle_panel[] p1 : sogui_ref.middlemiddle_ref.panels) {
        for (ServoOrchestratorGUI_middlemiddle_panel p : p1) {
          if (p != null) {
            bw.write(p.type + "~" + p.id + "~" + p.channel_name.getText() + "~" + p.servo_start.getText() + "~" + p.servo_channelid.getText() + "~" + p.servo_goal.getText() + "~"
                + p.servo_min.getText() + "~" + p.servo_max.getText());
          } else {
            bw.write("null");
          }
          bw.newLine();
        }
      }
      bw.close();
    } catch (IOException e) {
      Logging.logError(e);
    }
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(ServoOrchestrator.class.getCanonicalName());
    meta.addDescription("organize your Servo-movements");
    meta.addCategory("motor", "control", "display");

    return meta;
  }

}
