package org.myrobotlab.service;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * InMoovGestureCreator - This is a helper service to create gestures for the
 * InMoov It has a swing based gui that allows you to set servo angles on the
 * InMoov to create new gestures.
 *
 * @author LunDev (github), Ma. Vo. (MyRobotlab)
 */
public class InMoovGestureCreator extends Service {

  public static class FrameItemHolder {
    int rthumb, rindex, rmajeure, rringfinger, rpinky, rwrist;
    int rbicep, rrotate, rshoulder, romoplate;
    int lthumb, lindex, lmajeure, lringfinger, lpinky, lwrist;
    int lbicep, lrotate, lshoulder, lomoplate;
    int neck, rothead, eyeX, eyeY, jaw;
    int topStom, midStom, lowStom;
    double rthumbspeed, rindexspeed, rmajeurespeed, rringfingerspeed, rpinkyspeed, rwristspeed;
    double rbicepspeed, rrotatespeed, rshoulderspeed, romoplatespeed;
    double lthumbspeed, lindexspeed, lmajeurespeed, lringfingerspeed, lpinkyspeed, lwristspeed;
    double lbicepspeed, lrotatespeed, lshoulderspeed, lomoplatespeed;
    double neckspeed, rotheadspeed, eyeXspeed, eyeYspeed, jawspeed;
    double topStomspeed, midStomspeed, lowStomspeed;
    int sleep;
    String speech;
    String name;
  }

  public static class PythonItemHolder {
    String code;
    boolean modifyable;
    boolean function;
    boolean notfunction;
  }

  public static class ServoItemHolder {
    public JLabel fin;
    public JLabel min;
    public JLabel res;
    public JLabel max;
    public JSlider sli;
    public JLabel akt;
    public JTextField spe;
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovGestureCreator.class);

  transient ServoItemHolder[][] servoitemholder;

  transient ArrayList<FrameItemHolder> frameitemholder;

  transient ArrayList<PythonItemHolder> pythonitemholder;

  boolean[] tabs_main_checkbox_states;

  boolean moverealtime = false;
  InMoov i01;

  String pythonscript;

  String pythonname;

  String referencename;

  public static void main(String[] args) throws InterruptedException {

    LoggingFactory.init(Level.INFO);
    try {

      Runtime.start("gui", "SwingGui");
      Runtime.start("inmoovgesturecreator", "InMoovGestureCreator");

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public InMoovGestureCreator(String n) {
    super(n);
    // intializing variables
    servoitemholder = new ServoItemHolder[6][];
    frameitemholder = new ArrayList<FrameItemHolder>();
    pythonitemholder = new ArrayList<PythonItemHolder>();

    tabs_main_checkbox_states = new boolean[6];
  }

  public void control_addgest(JList control_list, JTextField control_gestname, JTextField control_funcname) {
    // Add the current gesture to the script (button bottom-left)
    String defname = control_funcname.getText();
    String gestname = control_gestname.getText();

    String code = "";
    for (FrameItemHolder fih : frameitemholder) {
      String code1;
      if (fih.sleep != -1) {
        code1 = "    sleep(" + fih.sleep + ")\n";
      } else if (fih.speech != null) {
        code1 = "    " + pythonname + ".mouth.speakBlocking(\"" + fih.speech + "\")\n";
      } else if (fih.name != null) {
        String code11 = "";
        String code12 = "";
        String code13 = "";
        String code14 = "";
        String code15 = "";
        String code16 = "";
        if (tabs_main_checkbox_states[0]) {
          code11 = "    " + pythonname + ".moveHead(" + fih.neck + "," + fih.rothead + "," + fih.eyeX + "," + fih.eyeY + "," + fih.jaw + ")\n";
        }
        if (tabs_main_checkbox_states[1]) {
          code12 = "    " + pythonname + ".moveArm(\"left\"," + fih.lbicep + "," + fih.lrotate + "," + fih.lshoulder + "," + fih.lomoplate + ")\n";
        }
        if (tabs_main_checkbox_states[2]) {
          code13 = "    " + pythonname + ".moveArm(\"right\"," + fih.rbicep + "," + fih.rrotate + "," + fih.rshoulder + "," + fih.romoplate + ")\n";
        }
        if (tabs_main_checkbox_states[3]) {
          code14 = "    " + pythonname + ".moveHand(\"left\"," + fih.lthumb + "," + fih.lindex + "," + fih.lmajeure + "," + fih.lringfinger + "," + fih.lpinky + "," + fih.lwrist
              + ")\n";
        }
        if (tabs_main_checkbox_states[4]) {
          code15 = "    " + pythonname + ".moveHand(\"right\"," + fih.rthumb + "," + fih.rindex + "," + fih.rmajeure + "," + fih.rringfinger + "," + fih.rpinky + "," + fih.rwrist
              + ")\n";
        }
        if (tabs_main_checkbox_states[5]) {
          code16 = "    " + pythonname + ".moveTorso(" + fih.topStom + "," + fih.midStom + "," + fih.lowStom + ")\n";
        }
        code1 = code11 + code12 + code13 + code14 + code15 + code16;
      } else {
        String code11 = "";
        String code12 = "";
        String code13 = "";
        String code14 = "";
        String code15 = "";
        String code16 = "";
        if (tabs_main_checkbox_states[0]) {
          code11 = "    " + pythonname + ".setHeadSpeed(" + fih.neckspeed + "," + fih.rotheadspeed + "," + fih.eyeXspeed + "," + fih.eyeYspeed + "," + fih.jawspeed + ")\n";
        }
        if (tabs_main_checkbox_states[1]) {
          code12 = "    " + pythonname + ".setArmSpeed(\"left\"," + fih.lbicepspeed + "," + fih.lrotatespeed + "," + fih.lshoulderspeed + "," + fih.lomoplatespeed + ")\n";
        }
        if (tabs_main_checkbox_states[2]) {
          code13 = "    " + pythonname + ".setArmSpeed(\"right\"," + fih.rbicepspeed + "," + fih.rrotatespeed + "," + fih.rshoulderspeed + "," + fih.romoplatespeed + ")\n";
        }
        if (tabs_main_checkbox_states[3]) {
          code14 = "    " + pythonname + ".setHandSpeed(\"left\"," + fih.lthumbspeed + "," + fih.lindexspeed + "," + fih.lmajeurespeed + "," + fih.lringfingerspeed + ","
              + fih.lpinkyspeed + "," + fih.lwristspeed + ")\n";
        }
        if (tabs_main_checkbox_states[4]) {
          code15 = "    " + pythonname + ".setHandSpeed(\"right\"," + fih.rthumbspeed + "," + fih.rindexspeed + "," + fih.rmajeurespeed + "," + fih.rringfingerspeed + ","
              + fih.rpinkyspeed + "," + fih.rwristspeed + ")\n";
        }
        if (tabs_main_checkbox_states[5]) {
          code16 = "    " + pythonname + ".setTorsoSpeed(" + fih.topStomspeed + "," + fih.midStomspeed + "," + fih.lowStomspeed + ")\n";
        }
        code1 = code11 + code12 + code13 + code14 + code15 + code16;
      }
      code = code + code1;
    }
    String finalcode = "def " + defname + "():\n" + code;

    String insert = "ear.addCommand(\"" + gestname + "\", \"python\", \"" + defname + "\")";
    int posear = pythonscript.lastIndexOf("ear.addCommand");
    int pos = pythonscript.indexOf("\n", posear);
    pythonscript = pythonscript.substring(0, pos) + "\n" + insert + pythonscript.substring(pos, pythonscript.length());

    pythonscript = pythonscript + "\n" + finalcode;

    parsescript(control_list);
  }

  public void control_connect(JButton control_connect) {
    // Connect / Disconnect to / from the InMoov service (button
    // bottom-left)
    if (control_connect.getText().equals("Connect")) {
      if (referencename == null) {
        referencename = "i01";
      }
      i01 = (InMoov) Runtime.getService(referencename);
      control_connect.setText("Disconnect");
    } else {
      i01 = null;
      control_connect.setText("Connect");
    }
  }

  public void control_loadgest(JList control_list, JList framelist, JTextField control_gestname, JTextField control_funcname) {
    // Load the current gesture from the script (button bottom-left)
    int posl = control_list.getSelectedIndex();

    if (posl != -1) {
      if (pythonitemholder.get(posl).modifyable) {
        frameitemholder.clear();

        String defname = null;

        String code = pythonitemholder.get(posl).code;
        String[] codesplit = code.split("\n");
        FrameItemHolder fih = null;
        boolean ismove = false;
        boolean isspeed = false;
        boolean head = false;
        boolean rhand = false;
        boolean lhand = false;
        boolean rarm = false;
        boolean larm = false;
        boolean torso = false;
        boolean keepgoing = true;
        int pos = 0;
        while (keepgoing) {
          if (fih == null) {
            fih = new FrameItemHolder();
          }
          String line;
          if (pos < codesplit.length) {
            line = codesplit[pos];
          } else {
            line = "pweicmfh - only one run";
            keepgoing = false;
          }
          String linewithoutspace = line.replace(" ", "");
          if (linewithoutspace.equals("")) {
            pos++;
            continue;
          }
          String line2 = line.replace(" ", "");
          if (!(ismove) && !(isspeed)) {
            if (line2.startsWith("def")) {
              String defn = line.substring(line.indexOf(" ") + 1, line.lastIndexOf("():"));
              defname = defn;
              pos++;
            } else if (line2.startsWith("sleep")) {
              String sleeptime = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
              fih.sleep = Integer.parseInt(sleeptime);
              fih.speech = null;
              fih.name = null;
              frameitemholder.add(fih);
              fih = null;
              pos++;
            } else if (line2.startsWith(pythonname)) {
              if (line2.startsWith(pythonname + ".mouth.speak")) {
                fih.sleep = -1;
                fih.speech = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
                fih.name = null;
                frameitemholder.add(fih);
                fih = null;
                pos++;
              } else if (line2.startsWith(pythonname + ".move")) {
                ismove = true;
                String good = line2.substring(line2.indexOf("(") + 1, line2.lastIndexOf(")"));
                String[] goodsplit = good.split(",");
                if (line2.startsWith(pythonname + ".moveHead")) {
                  fih.neck = Integer.parseInt(goodsplit[0]);
                  fih.rothead = Integer.parseInt(goodsplit[1]);
                  if (goodsplit.length > 2) {
                    fih.eyeX = Integer.parseInt(goodsplit[2]);
                    fih.eyeY = Integer.parseInt(goodsplit[3]);
                    fih.jaw = Integer.parseInt(goodsplit[4]);
                  } else {
                    fih.eyeX = 90;
                    fih.eyeY = 90;
                    fih.jaw = 90;
                  }
                  head = true;
                  pos++;
                } else if (line2.startsWith(pythonname + ".moveHand")) {
                  String gs = goodsplit[0];
                  String side = gs.substring(gs.indexOf("\"") + 1, gs.lastIndexOf("\""));
                  if (side.equals("right")) {
                    fih.rthumb = Integer.parseInt(goodsplit[1]);
                    fih.rindex = Integer.parseInt(goodsplit[2]);
                    fih.rmajeure = Integer.parseInt(goodsplit[3]);
                    fih.rringfinger = Integer.parseInt(goodsplit[4]);
                    fih.rpinky = Integer.parseInt(goodsplit[5]);
                    if (goodsplit.length > 6) {
                      fih.rwrist = Integer.parseInt(goodsplit[6]);
                    } else {
                      fih.rwrist = 90;
                    }
                    rhand = true;
                    pos++;
                  } else if (side.equals("left")) {
                    fih.lthumb = Integer.parseInt(goodsplit[1]);
                    fih.lindex = Integer.parseInt(goodsplit[2]);
                    fih.lmajeure = Integer.parseInt(goodsplit[3]);
                    fih.lringfinger = Integer.parseInt(goodsplit[4]);
                    fih.lpinky = Integer.parseInt(goodsplit[5]);
                    if (goodsplit.length > 6) {
                      fih.lwrist = Integer.parseInt(goodsplit[6]);
                    } else {
                      fih.lwrist = 90;
                    }
                    lhand = true;
                    pos++;
                  }
                } else if (line2.startsWith(pythonname + ".moveArm")) {
                  String gs = goodsplit[0];
                  String side = gs.substring(gs.indexOf("\"") + 1, gs.lastIndexOf("\""));
                  if (side.equals("right")) {
                    fih.rbicep = Integer.parseInt(goodsplit[1]);
                    fih.rrotate = Integer.parseInt(goodsplit[2]);
                    fih.rshoulder = Integer.parseInt(goodsplit[3]);
                    fih.romoplate = Integer.parseInt(goodsplit[4]);
                    rarm = true;
                    pos++;
                  } else if (side.equals("left")) {
                    fih.lbicep = Integer.parseInt(goodsplit[1]);
                    fih.lrotate = Integer.parseInt(goodsplit[2]);
                    fih.lshoulder = Integer.parseInt(goodsplit[3]);
                    fih.lomoplate = Integer.parseInt(goodsplit[4]);
                    larm = true;
                    pos++;
                  }
                } else if (line2.startsWith(pythonname + ".moveTorso")) {
                  fih.topStom = Integer.parseInt(goodsplit[0]);
                  fih.midStom = Integer.parseInt(goodsplit[1]);
                  fih.lowStom = Integer.parseInt(goodsplit[2]);
                  torso = true;
                  pos++;
                }
              } else if (line2.startsWith(pythonname + ".set")) {
                isspeed = true;
                String good = line2.substring(line2.indexOf("(") + 1, line2.lastIndexOf(")"));
                String[] goodsplit = good.split(",");
                if (line2.startsWith(pythonname + ".setHeadSpeed")) {
                  fih.neckspeed = Float.parseFloat(goodsplit[0]);
                  fih.rotheadspeed = Float.parseFloat(goodsplit[1]);
                  if (goodsplit.length > 2) {
                    fih.eyeXspeed = Float.parseFloat(goodsplit[2]);
                    fih.eyeYspeed = Float.parseFloat(goodsplit[3]);
                    fih.jawspeed = Float.parseFloat(goodsplit[4]);
                  } else {
                    fih.eyeXspeed = 1.0f;
                    fih.eyeYspeed = 1.0f;
                    fih.jawspeed = 1.0f;
                  }
                  head = true;
                  pos++;
                } else if (line2.startsWith(pythonname + ".setHandSpeed")) {
                  String gs = goodsplit[0];
                  String side = gs.substring(gs.indexOf("\"") + 1, gs.lastIndexOf("\""));
                  if (side.equals("right")) {
                    fih.rthumbspeed = Float.parseFloat(goodsplit[1]);
                    fih.rindexspeed = Float.parseFloat(goodsplit[2]);
                    fih.rmajeurespeed = Float.parseFloat(goodsplit[3]);
                    fih.rringfingerspeed = Float.parseFloat(goodsplit[4]);
                    fih.rpinkyspeed = Float.parseFloat(goodsplit[5]);
                    if (goodsplit.length > 6) {
                      fih.rwristspeed = Float.parseFloat(goodsplit[6]);
                    } else {
                      fih.rwristspeed = 1.0f;
                    }
                    rhand = true;
                    pos++;
                  } else if (side.equals("left")) {
                    fih.lthumbspeed = Float.parseFloat(goodsplit[1]);
                    fih.lindexspeed = Float.parseFloat(goodsplit[2]);
                    fih.lmajeurespeed = Float.parseFloat(goodsplit[3]);
                    fih.lringfingerspeed = Float.parseFloat(goodsplit[4]);
                    fih.lpinkyspeed = Float.parseFloat(goodsplit[5]);
                    if (goodsplit.length > 6) {
                      fih.lwristspeed = Float.parseFloat(goodsplit[6]);
                    } else {
                      fih.lwristspeed = 1.0f;
                    }
                    lhand = true;
                    pos++;
                  }
                } else if (line2.startsWith(pythonname + ".setArmSpeed")) {
                  String gs = goodsplit[0];
                  String side = gs.substring(gs.indexOf("\"") + 1, gs.lastIndexOf("\""));
                  if (side.equals("right")) {
                    fih.rbicepspeed = Float.parseFloat(goodsplit[1]);
                    fih.rrotatespeed = Float.parseFloat(goodsplit[2]);
                    fih.rshoulderspeed = Float.parseFloat(goodsplit[3]);
                    fih.romoplatespeed = Float.parseFloat(goodsplit[4]);
                    rarm = true;
                    pos++;
                  } else if (side.equals("left")) {
                    fih.lbicepspeed = Float.parseFloat(goodsplit[1]);
                    fih.lrotatespeed = Float.parseFloat(goodsplit[2]);
                    fih.lshoulderspeed = Float.parseFloat(goodsplit[3]);
                    fih.lomoplatespeed = Float.parseFloat(goodsplit[4]);
                    larm = true;
                    pos++;
                  }
                } else if (line2.startsWith(pythonname + ".setTorsoSpeed")) {
                  fih.topStomspeed = Float.parseFloat(goodsplit[0]);
                  fih.midStomspeed = Float.parseFloat(goodsplit[1]);
                  fih.lowStomspeed = Float.parseFloat(goodsplit[2]);
                  torso = true;
                  pos++;
                }
              }
            }
          } else if (ismove && !(isspeed)) {
            if (line2.startsWith(pythonname + ".move")) {
              String good = line2.substring(line2.indexOf("(") + 1, line2.lastIndexOf(")"));
              String[] goodsplit = good.split(",");
              if (line2.startsWith(pythonname + ".moveHead")) {
                fih.neck = Integer.parseInt(goodsplit[0]);
                fih.rothead = Integer.parseInt(goodsplit[1]);
                if (goodsplit.length > 2) {
                  fih.eyeX = Integer.parseInt(goodsplit[2]);
                  fih.eyeY = Integer.parseInt(goodsplit[3]);
                  fih.jaw = Integer.parseInt(goodsplit[4]);
                } else {
                  fih.eyeX = 90;
                  fih.eyeY = 90;
                  fih.jaw = 90;
                }
                head = true;
                pos++;
              } else if (line2.startsWith(pythonname + ".moveHand")) {
                String gs = goodsplit[0];
                String side = gs.substring(gs.indexOf("\"") + 1, gs.lastIndexOf("\""));
                if (side.equals("right")) {
                  fih.rthumb = Integer.parseInt(goodsplit[1]);
                  fih.rindex = Integer.parseInt(goodsplit[2]);
                  fih.rmajeure = Integer.parseInt(goodsplit[3]);
                  fih.rringfinger = Integer.parseInt(goodsplit[4]);
                  fih.rpinky = Integer.parseInt(goodsplit[5]);
                  if (goodsplit.length > 6) {
                    fih.rwrist = Integer.parseInt(goodsplit[6]);
                  } else {
                    fih.rwrist = 90;
                  }
                  rhand = true;
                  pos++;
                } else if (side.equals("left")) {
                  fih.lthumb = Integer.parseInt(goodsplit[1]);
                  fih.lindex = Integer.parseInt(goodsplit[2]);
                  fih.lmajeure = Integer.parseInt(goodsplit[3]);
                  fih.lringfinger = Integer.parseInt(goodsplit[4]);
                  fih.lpinky = Integer.parseInt(goodsplit[5]);
                  if (goodsplit.length > 6) {
                    fih.lwrist = Integer.parseInt(goodsplit[6]);
                  } else {
                    fih.lwrist = 90;
                  }
                  lhand = true;
                  pos++;
                }
              } else if (line2.startsWith(pythonname + ".moveArm")) {
                String gs = goodsplit[0];
                String side = gs.substring(gs.indexOf("\"") + 1, gs.lastIndexOf("\""));
                if (side.equals("right")) {
                  fih.rbicep = Integer.parseInt(goodsplit[1]);
                  fih.rrotate = Integer.parseInt(goodsplit[2]);
                  fih.rshoulder = Integer.parseInt(goodsplit[3]);
                  fih.romoplate = Integer.parseInt(goodsplit[4]);
                  rarm = true;
                  pos++;
                } else if (side.equals("left")) {
                  fih.lbicep = Integer.parseInt(goodsplit[1]);
                  fih.lrotate = Integer.parseInt(goodsplit[2]);
                  fih.lshoulder = Integer.parseInt(goodsplit[3]);
                  fih.lomoplate = Integer.parseInt(goodsplit[4]);
                  larm = true;
                  pos++;
                }
              } else if (line2.startsWith(pythonname + ".moveTorso")) {
                fih.topStom = Integer.parseInt(goodsplit[0]);
                fih.midStom = Integer.parseInt(goodsplit[1]);
                fih.lowStom = Integer.parseInt(goodsplit[2]);
                torso = true;
                pos++;
              }
            } else {
              if (!head) {
                fih.neck = 90;
                fih.rothead = 90;
                fih.eyeX = 90;
                fih.eyeY = 90;
                fih.jaw = 90;
              }
              if (!rhand) {
                fih.rthumb = 90;
                fih.rindex = 90;
                fih.rmajeure = 90;
                fih.rringfinger = 90;
                fih.rpinky = 90;
                fih.rwrist = 90;
              }
              if (!lhand) {
                fih.lthumb = 90;
                fih.lindex = 90;
                fih.lmajeure = 90;
                fih.lringfinger = 90;
                fih.lpinky = 90;
                fih.lwrist = 90;
              }
              if (!rarm) {
                fih.rbicep = 90;
                fih.rrotate = 90;
                fih.rshoulder = 90;
                fih.romoplate = 90;
              }
              if (!larm) {
                fih.lbicep = 90;
                fih.lrotate = 90;
                fih.lshoulder = 90;
                fih.lomoplate = 90;
              }
              if (!torso) {
                fih.topStom = 90;
                fih.midStom = 90;
                fih.lowStom = 90;
              }
              fih.sleep = -1;
              fih.speech = null;
              fih.name = "SEQ";
              frameitemholder.add(fih);
              fih = null;
              ismove = false;
              head = false;
              rhand = false;
              lhand = false;
              rarm = false;
              larm = false;
              torso = false;
            }
          } else if (!(ismove) && isspeed) {
            if (line2.startsWith(pythonname + ".set")) {
              String good = line2.substring(line2.indexOf("(") + 1, line2.lastIndexOf(")"));
              String[] goodsplit = good.split(",");
              if (line2.startsWith(pythonname + ".setHeadSpeed")) {
                fih.neckspeed = Float.parseFloat(goodsplit[0]);
                fih.rotheadspeed = Float.parseFloat(goodsplit[1]);
                if (goodsplit.length > 2) {
                  fih.eyeXspeed = Float.parseFloat(goodsplit[2]);
                  fih.eyeYspeed = Float.parseFloat(goodsplit[3]);
                  fih.jawspeed = Float.parseFloat(goodsplit[4]);
                } else {
                  fih.eyeXspeed = 1.0f;
                  fih.eyeYspeed = 1.0f;
                  fih.jawspeed = 1.0f;
                }
                head = true;
                pos++;
              } else if (line2.startsWith(pythonname + ".setHandSpeed")) {
                String gs = goodsplit[0];
                String side = gs.substring(gs.indexOf("\"") + 1, gs.lastIndexOf("\""));
                if (side.equals("right")) {
                  fih.rthumbspeed = Float.parseFloat(goodsplit[1]);
                  fih.rindexspeed = Float.parseFloat(goodsplit[2]);
                  fih.rmajeurespeed = Float.parseFloat(goodsplit[3]);
                  fih.rringfingerspeed = Float.parseFloat(goodsplit[4]);
                  fih.rpinkyspeed = Float.parseFloat(goodsplit[5]);
                  if (goodsplit.length > 6) {
                    fih.rwristspeed = Float.parseFloat(goodsplit[6]);
                  } else {
                    fih.rwristspeed = 1.0f;
                  }
                  rhand = true;
                  pos++;
                } else if (side.equals("left")) {
                  fih.lthumbspeed = Float.parseFloat(goodsplit[1]);
                  fih.lindexspeed = Float.parseFloat(goodsplit[2]);
                  fih.lmajeurespeed = Float.parseFloat(goodsplit[3]);
                  fih.lringfingerspeed = Float.parseFloat(goodsplit[4]);
                  fih.lpinkyspeed = Float.parseFloat(goodsplit[5]);
                  if (goodsplit.length > 6) {
                    fih.lwristspeed = Float.parseFloat(goodsplit[6]);
                  } else {
                    fih.lwristspeed = 1.0f;
                  }
                  lhand = true;
                  pos++;
                }
              } else if (line2.startsWith(pythonname + ".setArmSpeed")) {
                String gs = goodsplit[0];
                String side = gs.substring(gs.indexOf("\"") + 1, gs.lastIndexOf("\""));
                if (side.equals("right")) {
                  fih.rbicepspeed = Float.parseFloat(goodsplit[1]);
                  fih.rrotatespeed = Float.parseFloat(goodsplit[2]);
                  fih.rshoulderspeed = Float.parseFloat(goodsplit[3]);
                  fih.romoplatespeed = Float.parseFloat(goodsplit[4]);
                  rarm = true;
                  pos++;
                } else if (side.equals("left")) {
                  fih.lbicepspeed = Float.parseFloat(goodsplit[1]);
                  fih.lrotatespeed = Float.parseFloat(goodsplit[2]);
                  fih.lshoulderspeed = Float.parseFloat(goodsplit[3]);
                  fih.lomoplatespeed = Float.parseFloat(goodsplit[4]);
                  larm = true;
                  pos++;
                }
              } else if (line2.startsWith(pythonname + ".setTorsoSpeed")) {
                fih.topStomspeed = Float.parseFloat(goodsplit[0]);
                fih.midStomspeed = Float.parseFloat(goodsplit[1]);
                fih.lowStomspeed = Float.parseFloat(goodsplit[2]);
                torso = true;
                pos++;
              }
            } else {
              if (!head) {
                fih.neckspeed = 1.0f;
                fih.rotheadspeed = 1.0f;
                fih.eyeXspeed = 1.0f;
                fih.eyeYspeed = 1.0f;
                fih.jawspeed = 1.0f;
              }
              if (!rhand) {
                fih.rthumbspeed = 1.0f;
                fih.rindexspeed = 1.0f;
                fih.rmajeurespeed = 1.0f;
                fih.rringfingerspeed = 1.0f;
                fih.rpinkyspeed = 1.0f;
                fih.rwristspeed = 1.0f;
              }
              if (!lhand) {
                fih.lthumbspeed = 1.0f;
                fih.lindexspeed = 1.0f;
                fih.lmajeurespeed = 1.0f;
                fih.lringfingerspeed = 1.0f;
                fih.lpinkyspeed = 1.0f;
                fih.lwristspeed = 1.0f;
              }
              if (!rarm) {
                fih.rbicepspeed = 1.0f;
                fih.rrotatespeed = 1.0f;
                fih.rshoulderspeed = 1.0f;
                fih.romoplatespeed = 1.0f;
              }
              if (!larm) {
                fih.lbicepspeed = 1.0f;
                fih.lrotatespeed = 1.0f;
                fih.lshoulderspeed = 1.0f;
                fih.lomoplatespeed = 1.0f;
              }
              if (!torso) {
                fih.topStomspeed = 1.0f;
                fih.midStomspeed = 1.0f;
                fih.lowStomspeed = 1.0f;
              }
              fih.sleep = -1;
              fih.speech = null;
              fih.name = null;
              frameitemholder.add(fih);
              fih = null;
              isspeed = false;
              head = false;
              rhand = false;
              lhand = false;
              rarm = false;
              larm = false;
              torso = false;
            }
          } else {
            // this shouldn't be reached
            // ismove & isspeed true
            // wrong
          }
        }

        framelistact(framelist);

        int defnamepos = pythonscript.indexOf(defname);
        int earpos1 = pythonscript.lastIndexOf("\n", defnamepos);
        int earpos2 = pythonscript.indexOf("\n", defnamepos);
        String earline = pythonscript.substring(earpos1 + 1, earpos2);
        if (earline.startsWith("ear.addCommand")) {
          String good = earline.substring(earline.indexOf("("), earline.lastIndexOf(")"));
          String[] goodsplit = good.split(",");

          String funcnamedirty = goodsplit[0];
          String funcname = funcnamedirty.substring(funcnamedirty.indexOf("\"") + 1, funcnamedirty.lastIndexOf("\""));

          control_gestname.setText(funcname);
          control_funcname.setText(defname);
        }
      }
    }
  }

  public void control_loadscri(JList control_list) {
    // Load the Python-Script (out Python-Service) (button bottom-left)
   // Python python = (Python) Runtime.getService("python");
   //  Script script = python.getScript();
    pythonscript = "not supported";//script.getCode();

    parsescript(control_list);
  }

  public void control_removegest(JList control_list) {
    // Remove the selected gesture from the script (button bottom-left)
    int posl = control_list.getSelectedIndex();

    if (posl != -1) {

      if (pythonitemholder.get(posl).function && !pythonitemholder.get(posl).notfunction) {

        String codeold = pythonitemholder.get(posl).code;
        String defnameold = codeold.substring(codeold.indexOf("def ") + 4, codeold.indexOf("():"));

        int olddefpos = pythonscript.indexOf(defnameold);
        int pos1 = pythonscript.lastIndexOf("\n", olddefpos);
        int pos2 = pythonscript.indexOf("\n", olddefpos);
        pythonscript = pythonscript.substring(0, pos1) + pythonscript.substring(pos2, pythonscript.length());

        int posscript = pythonscript.lastIndexOf(defnameold);
        int posscriptnextdef = pythonscript.indexOf("def", posscript);
        if (posscriptnextdef == -1) {
          posscriptnextdef = pythonscript.length();
        }

        pythonscript = pythonscript.substring(0, posscript - 4) + pythonscript.substring(posscriptnextdef - 1, pythonscript.length());

        parsescript(control_list);
      }
    }
  }

  public void control_savescri() {
    // Save the Python-Script (in Python-Service) (button bottom-left)
    JFrame frame = new JFrame();
    JTextArea textarea = new JTextArea();
    textarea.setText(pythonscript);
    textarea.setEditable(false);
    textarea.setLineWrap(true);
    JScrollPane scrollpane = new JScrollPane(textarea);
    scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    frame.add(scrollpane);
    frame.pack();
    frame.setVisible(true);
  }

  public void control_testgest() {
    // test (execute) the created gesture (button bottom-left)
    if (i01 != null) {
      for (FrameItemHolder fih : frameitemholder) {
        if (fih.sleep != -1) {
          sleep(fih.sleep);
        } else if (fih.speech != null) {
          try {
            i01.mouth.speakBlocking(fih.speech);
          } catch (Exception e) {
            Logging.logError(e);
          }
        } else if (fih.name != null) {
          if (tabs_main_checkbox_states[0]) {
            i01.moveHead(fih.neck, fih.rothead, fih.eyeX, fih.eyeY, fih.jaw);
          }
          if (tabs_main_checkbox_states[1]) {
            i01.moveArm("left", fih.lbicep, fih.lrotate, fih.lshoulder, fih.lomoplate);
          }
          if (tabs_main_checkbox_states[2]) {
            i01.moveArm("right", fih.rbicep, fih.rrotate, fih.rshoulder, fih.romoplate);
          }
          if (tabs_main_checkbox_states[3]) {
            i01.moveHand("left", fih.lthumb, fih.lindex, fih.lmajeure, fih.lringfinger, fih.lpinky, (double) fih.lwrist);
          }
          if (tabs_main_checkbox_states[4]) {
            i01.moveHand("right", fih.rthumb, fih.rindex, fih.rmajeure, fih.rringfinger, fih.rpinky, (double) fih.rwrist);
          }
          if (tabs_main_checkbox_states[5]) {
            i01.moveTorso(fih.topStom, fih.midStom, fih.lowStom);
          }
        } else {
          if (tabs_main_checkbox_states[0]) {
            i01.setHeadSpeed(fih.neckspeed, fih.rotheadspeed, fih.eyeXspeed, fih.eyeYspeed, fih.jawspeed);
          }
          if (tabs_main_checkbox_states[1]) {
            i01.setArmSpeed("left", fih.lbicepspeed, fih.lrotatespeed, fih.lshoulderspeed, fih.lomoplatespeed);
          }
          if (tabs_main_checkbox_states[2]) {
            i01.setArmSpeed("right", fih.rbicepspeed, fih.rrotatespeed, fih.rshoulderspeed, fih.romoplatespeed);
          }
          if (tabs_main_checkbox_states[3]) {
            i01.setHandSpeed("left", fih.lthumbspeed, fih.lindexspeed, fih.lmajeurespeed, fih.lringfingerspeed, fih.lpinkyspeed, fih.lwristspeed);
          }
          if (tabs_main_checkbox_states[4]) {
            i01.setHandSpeed("right", fih.rthumbspeed, fih.rindexspeed, fih.rmajeurespeed, fih.rringfingerspeed, fih.rpinkyspeed, fih.rwristspeed);
          }
          if (tabs_main_checkbox_states[5]) {
            i01.setTorsoSpeed(fih.topStomspeed, fih.midStomspeed, fih.lowStomspeed);
          }
        }
      }
    }
  }

  public void control_updategest(JList control_list, JTextField control_gestname, JTextField control_funcname) {
    // Update the current gesture in the script (button bottom-left)
    int posl = control_list.getSelectedIndex();

    if (posl != -1) {

      if (pythonitemholder.get(posl).function && !pythonitemholder.get(posl).notfunction) {

        String codeold = pythonitemholder.get(posl).code;
        String defnameold = codeold.substring(codeold.indexOf("def ") + 4, codeold.indexOf("():"));

        String defname = control_funcname.getText();
        String gestname = control_gestname.getText();

        String code = "";
        for (FrameItemHolder fih : frameitemholder) {
          String code1;
          if (fih.sleep != -1) {
            code1 = "    sleep(" + fih.sleep + ")\n";
          } else if (fih.speech != null) {
            code1 = "    " + pythonname + ".mouth.speakBlocking(\"" + fih.speech + "\")\n";
          } else if (fih.name != null) {
            String code11 = "";
            String code12 = "";
            String code13 = "";
            String code14 = "";
            String code15 = "";
            String code16 = "";
            if (tabs_main_checkbox_states[0]) {
              code11 = "    " + pythonname + ".moveHead(" + fih.neck + "," + fih.rothead + "," + fih.eyeX + "," + fih.eyeY + "," + fih.jaw + ")\n";
            }
            if (tabs_main_checkbox_states[1]) {
              code12 = "    " + pythonname + ".moveArm(\"left\"," + fih.lbicep + "," + fih.lrotate + "," + fih.lshoulder + "," + fih.lomoplate + ")\n";
            }
            if (tabs_main_checkbox_states[2]) {
              code13 = "    " + pythonname + ".moveArm(\"right\"," + fih.rbicep + "," + fih.rrotate + "," + fih.rshoulder + "," + fih.romoplate + ")\n";
            }
            if (tabs_main_checkbox_states[3]) {
              code14 = "    " + pythonname + ".moveHand(\"left\"," + fih.lthumb + "," + fih.lindex + "," + fih.lmajeure + "," + fih.lringfinger + "," + fih.lpinky + ","
                  + fih.lwrist + ")\n";
            }
            if (tabs_main_checkbox_states[4]) {
              code15 = "    " + pythonname + ".moveHand(\"right\"," + fih.rthumb + "," + fih.rindex + "," + fih.rmajeure + "," + fih.rringfinger + "," + fih.rpinky + ","
                  + fih.rwrist + ")\n";
            }
            if (tabs_main_checkbox_states[5]) {
              code16 = "    " + pythonname + ".moveTorso(" + fih.topStom + "," + fih.midStom + "," + fih.lowStom + ")\n";
            }
            code1 = code11 + code12 + code13 + code14 + code15 + code16;
          } else {
            String code11 = "";
            String code12 = "";
            String code13 = "";
            String code14 = "";
            String code15 = "";
            String code16 = "";
            if (tabs_main_checkbox_states[0]) {
              code11 = "    " + pythonname + ".setHeadSpeed(" + fih.neckspeed + "," + fih.rotheadspeed + "," + fih.eyeXspeed + "," + fih.eyeYspeed + "," + fih.jawspeed + ")\n";
            }
            if (tabs_main_checkbox_states[1]) {
              code12 = "    " + pythonname + ".setArmSpeed(\"left\"," + fih.lbicepspeed + "," + fih.lrotatespeed + "," + fih.lshoulderspeed + "," + fih.lomoplatespeed + ")\n";
            }
            if (tabs_main_checkbox_states[2]) {
              code13 = "    " + pythonname + ".setArmSpeed(\"right\"," + fih.rbicepspeed + "," + fih.rrotatespeed + "," + fih.rshoulderspeed + "," + fih.romoplatespeed + ")\n";
            }
            if (tabs_main_checkbox_states[3]) {
              code14 = "    " + pythonname + ".setHandSpeed(\"left\"," + fih.lthumbspeed + "," + fih.lindexspeed + "," + fih.lmajeurespeed + "," + fih.lringfingerspeed + ","
                  + fih.lpinkyspeed + "," + fih.lwristspeed + ")\n";
            }
            if (tabs_main_checkbox_states[4]) {
              code15 = "    " + pythonname + ".setHandSpeed(\"right\"," + fih.rthumbspeed + "," + fih.rindexspeed + "," + fih.rmajeurespeed + "," + fih.rringfingerspeed + ","
                  + fih.rpinkyspeed + "," + fih.rwristspeed + ")\n";
            }
            if (tabs_main_checkbox_states[5]) {
              code16 = "    " + pythonname + ".setTorsoSpeed(" + fih.topStomspeed + "," + fih.midStomspeed + "," + fih.lowStomspeed + ")\n";
            }
            code1 = code11 + code12 + code13 + code14 + code15 + code16;
          }
          code = code + code1;
        }
        String finalcode = "def " + defname + "():\n" + code;

        String insert = "ear.addCommand(\"" + gestname + "\", \"python\", \"" + defname + "\")";
        int olddefpos = pythonscript.indexOf(defnameold);
        int pos1 = pythonscript.lastIndexOf("\n", olddefpos);
        int pos2 = pythonscript.indexOf("\n", olddefpos);
        pythonscript = pythonscript.substring(0, pos1) + "\n" + insert + pythonscript.substring(pos2, pythonscript.length());

        int posscript = pythonscript.lastIndexOf(defnameold);
        int posscriptnextdef = pythonscript.indexOf("def", posscript);
        if (posscriptnextdef == -1) {
          posscriptnextdef = pythonscript.length();
        }

        pythonscript = pythonscript.substring(0, posscript - 4) + "\n" + finalcode + pythonscript.substring(posscriptnextdef - 1, pythonscript.length());

        parsescript(control_list);
      }
    }
  }

  public void controllistact(JList control_list) {
    String[] listdata = new String[pythonitemholder.size()];
    for (int i = 0; i < pythonitemholder.size(); i++) {
      PythonItemHolder pih = pythonitemholder.get(i);

      String pre;
      if (!(pih.modifyable)) {
        pre = "X    ";
      } else {
        pre = "     ";
      }

      int he = 21;
      if (pih.code.length() < he) {
        he = pih.code.length();
      }

      String des = pih.code.substring(0, he);

      String displaytext = pre + des;
      listdata[i] = displaytext;
    }
    control_list.setListData(listdata);
  }

  public void frame_add(JList framelist, JTextField frame_add_textfield) {
    // Add a servo movement frame to the framelist (button bottom-right)
    FrameItemHolder fih = new FrameItemHolder();

    fih.rthumb = servoitemholder[0][0].sli.getValue();
    fih.rindex = servoitemholder[0][1].sli.getValue();
    fih.rmajeure = servoitemholder[0][2].sli.getValue();
    fih.rringfinger = servoitemholder[0][3].sli.getValue();
    fih.rpinky = servoitemholder[0][4].sli.getValue();
    fih.rwrist = servoitemholder[0][5].sli.getValue();

    fih.rbicep = servoitemholder[1][0].sli.getValue();
    fih.rrotate = servoitemholder[1][1].sli.getValue();
    fih.rshoulder = servoitemholder[1][2].sli.getValue();
    fih.romoplate = servoitemholder[1][3].sli.getValue();

    fih.lthumb = servoitemholder[2][0].sli.getValue();
    fih.lindex = servoitemholder[2][1].sli.getValue();
    fih.lmajeure = servoitemholder[2][2].sli.getValue();
    fih.lringfinger = servoitemholder[2][3].sli.getValue();
    fih.lpinky = servoitemholder[2][4].sli.getValue();
    fih.lwrist = servoitemholder[2][5].sli.getValue();

    fih.lbicep = servoitemholder[3][0].sli.getValue();
    fih.lrotate = servoitemholder[3][1].sli.getValue();
    fih.lshoulder = servoitemholder[3][2].sli.getValue();
    fih.lomoplate = servoitemholder[3][3].sli.getValue();

    fih.neck = servoitemholder[4][0].sli.getValue();
    fih.rothead = servoitemholder[4][1].sli.getValue();
    fih.eyeX = servoitemholder[4][2].sli.getValue();
    fih.eyeY = servoitemholder[4][3].sli.getValue();
    fih.jaw = servoitemholder[4][4].sli.getValue();

    fih.topStom = servoitemholder[5][0].sli.getValue();
    fih.midStom = servoitemholder[5][1].sli.getValue();
    fih.lowStom = servoitemholder[5][2].sli.getValue();

    fih.sleep = -1;
    fih.speech = null;
    fih.name = frame_add_textfield.getText();

    frameitemholder.add(fih);

    framelistact(framelist);
  }

  public void frame_addsleep(JList framelist, JTextField frame_addsleep_textfield) {
    // Add a sleep frame to the framelist (button bottom-right)
    FrameItemHolder fih = new FrameItemHolder();

    fih.sleep = Integer.parseInt(frame_addsleep_textfield.getText());
    fih.speech = null;
    fih.name = null;

    frameitemholder.add(fih);

    framelistact(framelist);
  }

  public void frame_addspeech(JList framelist, JTextField frame_addspeech_textfield) {
    // Add a speech frame to the framelist (button bottom-right)
    FrameItemHolder fih = new FrameItemHolder();

    fih.sleep = -1;
    fih.speech = frame_addspeech_textfield.getText();
    fih.name = null;

    frameitemholder.add(fih);

    framelistact(framelist);
  }

  public void frame_addspeed(JList framelist) {
    // Add a speed setting frame to the framelist (button bottom-right)
    FrameItemHolder fih = new FrameItemHolder();

    fih.rthumbspeed = Float.parseFloat(servoitemholder[0][0].spe.getText());
    fih.rindexspeed = Float.parseFloat(servoitemholder[0][1].spe.getText());
    fih.rmajeurespeed = Float.parseFloat(servoitemholder[0][2].spe.getText());
    fih.rringfingerspeed = Float.parseFloat(servoitemholder[0][3].spe.getText());
    fih.rpinkyspeed = Float.parseFloat(servoitemholder[0][4].spe.getText());
    fih.rwristspeed = Float.parseFloat(servoitemholder[0][5].spe.getText());

    fih.rbicepspeed = Float.parseFloat(servoitemholder[1][0].spe.getText());
    fih.rrotatespeed = Float.parseFloat(servoitemholder[1][1].spe.getText());
    fih.rshoulderspeed = Float.parseFloat(servoitemholder[1][2].spe.getText());
    fih.romoplatespeed = Float.parseFloat(servoitemholder[1][3].spe.getText());

    fih.lthumbspeed = Float.parseFloat(servoitemholder[2][0].spe.getText());
    fih.lindexspeed = Float.parseFloat(servoitemholder[2][1].spe.getText());
    fih.lmajeurespeed = Float.parseFloat(servoitemholder[2][2].spe.getText());
    fih.lringfingerspeed = Float.parseFloat(servoitemholder[2][3].spe.getText());
    fih.lpinkyspeed = Float.parseFloat(servoitemholder[2][4].spe.getText());
    fih.lwristspeed = Float.parseFloat(servoitemholder[2][5].spe.getText());

    fih.lbicepspeed = Float.parseFloat(servoitemholder[3][0].spe.getText());
    fih.lrotatespeed = Float.parseFloat(servoitemholder[3][1].spe.getText());
    fih.lshoulderspeed = Float.parseFloat(servoitemholder[3][2].spe.getText());
    fih.lomoplatespeed = Float.parseFloat(servoitemholder[3][3].spe.getText());

    fih.neckspeed = Float.parseFloat(servoitemholder[4][0].spe.getText());
    fih.rotheadspeed = Float.parseFloat(servoitemholder[4][1].spe.getText());
    fih.eyeXspeed = Float.parseFloat(servoitemholder[4][2].spe.getText());
    fih.eyeYspeed = Float.parseFloat(servoitemholder[4][3].spe.getText());
    fih.jawspeed = Float.parseFloat(servoitemholder[4][4].spe.getText());

    fih.topStomspeed = Float.parseFloat(servoitemholder[5][0].spe.getText());
    fih.midStomspeed = Float.parseFloat(servoitemholder[5][1].spe.getText());
    fih.lowStomspeed = Float.parseFloat(servoitemholder[5][2].spe.getText());

    fih.sleep = -1;
    fih.speech = null;
    fih.name = null;

    frameitemholder.add(fih);

    framelistact(framelist);
  }

  public void frame_copy(JList framelist) {
    // Copy this frame on the framelist (button bottom-right)
    int pos = framelist.getSelectedIndex();

    if (pos != -1) {
      FrameItemHolder fih = frameitemholder.get(pos);
      frameitemholder.add(fih);

      framelistact(framelist);
    }
  }

  public void frame_down(JList framelist) {
    // Move this frame one down on the framelist (button bottom-right)
    int pos = framelist.getSelectedIndex();

    if (pos != -1) {
      FrameItemHolder fih = frameitemholder.remove(pos);
      frameitemholder.add(pos + 1, fih);

      framelistact(framelist);
    }
  }

  public void frame_importminresmax() {
    // Import the Min- / Res- / Max- settings of your InMoov
    if (i01 != null) {
      for (int i1 = 0; i1 < servoitemholder.length; i1++) {
        for (int i2 = 0; i2 < servoitemholder[i1].length; i2++) {
          InMoovHand inmhand = null;
          InMoovArm inmarm = null;
          InMoovHead inmhead = null;
          InMoovTorso inmtorso = null;

          if (i1 == 0) {
            inmhand = i01.rightHand;
          } else if (i1 == 1) {
            inmarm = i01.rightArm;
          } else if (i1 == 2) {
            inmhand = i01.leftHand;
          } else if (i1 == 3) {
            inmarm = i01.rightArm;
          } else if (i1 == 4) {
            inmhead = i01.head;
          } else if (i1 == 5) {
            inmtorso = i01.torso;
          }

          Servo servo = null;

          if (i1 == 0 || i1 == 2) {
            if (i2 == 0) {
              servo = inmhand.thumb;
            } else if (i2 == 1) {
              servo = inmhand.index;
            } else if (i2 == 2) {
              servo = inmhand.majeure;
            } else if (i2 == 3) {
              servo = inmhand.ringFinger;
            } else if (i2 == 4) {
              servo = inmhand.pinky;
            } else if (i2 == 5) {
              servo = inmhand.wrist;
            }
          } else if (i1 == 1 || i1 == 3) {
            if (i2 == 0) {
              servo = inmarm.bicep;
            } else if (i2 == 1) {
              servo = inmarm.rotate;
            } else if (i2 == 2) {
              servo = inmarm.shoulder;
            } else if (i2 == 3) {
              servo = inmarm.omoplate;
            }
          } else if (i1 == 4) {
            if (i2 == 0) {
              servo = inmhead.neck;
            } else if (i2 == 1) {
              servo = inmhead.rothead;
            } else if (i2 == 2) {
              servo = inmhead.eyeX;
            } else if (i2 == 3) {
              servo = inmhead.eyeY;
            } else if (i2 == 4) {
              servo = inmhead.jaw;
            }
          } else if (i1 == 5) {
            if (i2 == 0) {
              servo = inmtorso.topStom;
            } else if (i2 == 1) {
              servo = inmtorso.midStom;
            } else if (i2 == 2) {
              servo = inmtorso.lowStom;
            }
          }

          Double min = servo.getMin();
          double res = servo.getRest();
          Double max = servo.getMax();

          servoitemholder[i1][i2].min.setText(min + "");
          servoitemholder[i1][i2].res.setText(res + "");
          servoitemholder[i1][i2].max.setText(max + "");
          // servoitemholder[i1][i2].sli.setMinimum(min);
          // servoitemholder[i1][i2].sli.setMaximum(max);
          // servoitemholder[i1][i2].sli.setValue(res);
        }
      }
    }
  }

  public void frame_load(JList framelist, JTextField frame_add_textfield, JTextField frame_addsleep_textfield, JTextField frame_addspeech_textfield) {
    // Load this frame from the framelist (button bottom-right)
    int pos = framelist.getSelectedIndex();

    if (pos != -1) {

      // sleep || speech || servo movement || speed setting
      if (frameitemholder.get(pos).sleep != -1) {
        frame_addsleep_textfield.setText(frameitemholder.get(pos).sleep + "");
      } else if (frameitemholder.get(pos).speech != null) {
        frame_addspeech_textfield.setText(frameitemholder.get(pos).speech);
      } else if (frameitemholder.get(pos).name != null) {
        servoitemholder[0][0].sli.setValue(frameitemholder.get(pos).rthumb);
        servoitemholder[0][1].sli.setValue(frameitemholder.get(pos).rindex);
        servoitemholder[0][2].sli.setValue(frameitemholder.get(pos).rmajeure);
        servoitemholder[0][3].sli.setValue(frameitemholder.get(pos).rringfinger);
        servoitemholder[0][4].sli.setValue(frameitemholder.get(pos).rpinky);
        servoitemholder[0][5].sli.setValue(frameitemholder.get(pos).rwrist);

        servoitemholder[1][0].sli.setValue(frameitemholder.get(pos).rbicep);
        servoitemholder[1][1].sli.setValue(frameitemholder.get(pos).rrotate);
        servoitemholder[1][2].sli.setValue(frameitemholder.get(pos).rshoulder);
        servoitemholder[1][3].sli.setValue(frameitemholder.get(pos).romoplate);

        servoitemholder[2][0].sli.setValue(frameitemholder.get(pos).lthumb);
        servoitemholder[2][1].sli.setValue(frameitemholder.get(pos).lindex);
        servoitemholder[2][2].sli.setValue(frameitemholder.get(pos).lmajeure);
        servoitemholder[2][3].sli.setValue(frameitemholder.get(pos).lringfinger);
        servoitemholder[2][4].sli.setValue(frameitemholder.get(pos).lpinky);
        servoitemholder[2][5].sli.setValue(frameitemholder.get(pos).lwrist);

        servoitemholder[3][0].sli.setValue(frameitemholder.get(pos).lbicep);
        servoitemholder[3][1].sli.setValue(frameitemholder.get(pos).lrotate);
        servoitemholder[3][2].sli.setValue(frameitemholder.get(pos).lshoulder);
        servoitemholder[3][3].sli.setValue(frameitemholder.get(pos).lomoplate);

        servoitemholder[4][0].sli.setValue(frameitemholder.get(pos).neck);
        servoitemholder[4][1].sli.setValue(frameitemholder.get(pos).rothead);
        servoitemholder[4][2].sli.setValue(frameitemholder.get(pos).eyeX);
        servoitemholder[4][3].sli.setValue(frameitemholder.get(pos).eyeY);
        servoitemholder[4][4].sli.setValue(frameitemholder.get(pos).jaw);

        servoitemholder[5][0].sli.setValue(frameitemholder.get(pos).topStom);
        servoitemholder[5][1].sli.setValue(frameitemholder.get(pos).midStom);
        servoitemholder[5][2].sli.setValue(frameitemholder.get(pos).lowStom);
        frame_add_textfield.setText(frameitemholder.get(pos).name);
      } else {
        servoitemholder[0][0].spe.setText(frameitemholder.get(pos).rthumbspeed + "");
        servoitemholder[0][1].spe.setText(frameitemholder.get(pos).rindexspeed + "");
        servoitemholder[0][2].spe.setText(frameitemholder.get(pos).rmajeurespeed + "");
        servoitemholder[0][3].spe.setText(frameitemholder.get(pos).rringfingerspeed + "");
        servoitemholder[0][4].spe.setText(frameitemholder.get(pos).rpinkyspeed + "");
        servoitemholder[0][5].spe.setText(frameitemholder.get(pos).rwristspeed + "");

        servoitemholder[1][0].spe.setText(frameitemholder.get(pos).rbicepspeed + "");
        servoitemholder[1][1].spe.setText(frameitemholder.get(pos).rrotatespeed + "");
        servoitemholder[1][2].spe.setText(frameitemholder.get(pos).rshoulderspeed + "");
        servoitemholder[1][3].spe.setText(frameitemholder.get(pos).romoplatespeed + "");

        servoitemholder[2][0].spe.setText(frameitemholder.get(pos).lthumbspeed + "");
        servoitemholder[2][1].spe.setText(frameitemholder.get(pos).lindexspeed + "");
        servoitemholder[2][2].spe.setText(frameitemholder.get(pos).lmajeurespeed + "");
        servoitemholder[2][3].spe.setText(frameitemholder.get(pos).lringfingerspeed + "");
        servoitemholder[2][4].spe.setText(frameitemholder.get(pos).lpinkyspeed + "");
        servoitemholder[2][5].spe.setText(frameitemholder.get(pos).lwristspeed + "");

        servoitemholder[3][0].spe.setText(frameitemholder.get(pos).lbicepspeed + "");
        servoitemholder[3][1].spe.setText(frameitemholder.get(pos).lrotatespeed + "");
        servoitemholder[3][2].spe.setText(frameitemholder.get(pos).lshoulderspeed + "");
        servoitemholder[3][3].spe.setText(frameitemholder.get(pos).lomoplatespeed + "");

        servoitemholder[4][0].spe.setText(frameitemholder.get(pos).neckspeed + "");
        servoitemholder[4][1].spe.setText(frameitemholder.get(pos).rotheadspeed + "");
        servoitemholder[4][2].spe.setText(frameitemholder.get(pos).eyeXspeed + "");
        servoitemholder[4][3].spe.setText(frameitemholder.get(pos).eyeYspeed + "");
        servoitemholder[4][4].spe.setText(frameitemholder.get(pos).jawspeed + "");

        servoitemholder[5][0].spe.setText(frameitemholder.get(pos).topStomspeed + "");
        servoitemholder[5][1].spe.setText(frameitemholder.get(pos).midStomspeed + "");
        servoitemholder[5][2].spe.setText(frameitemholder.get(pos).lowStomspeed + "");
      }
    }
  }

  public void frame_moverealtime(JCheckBox frame_moverealtime) {
    moverealtime = frame_moverealtime.isSelected();
  }

  public void frame_remove(JList framelist) {
    // Remove this frame from the framelist (button bottom-right)
    int pos = framelist.getSelectedIndex();
    if (pos != -1) {
      frameitemholder.remove(pos);

      framelistact(framelist);
    }
  }

  public void frame_test(JList framelist) {
    // Test this frame (execute)
    int pos = framelist.getSelectedIndex();
    if (i01 != null && pos != -1) {
      FrameItemHolder fih = frameitemholder.get(pos);

      // sleep || speech || servo movement || speed setting
      if (fih.sleep != -1) {
        sleep(fih.sleep);
      } else if (fih.speech != null) {
        try {
          i01.mouth.speakBlocking(fih.speech);
        } catch (Exception e) {
          Logging.logError(e);
        }
      } else if (fih.name != null) {
        if (tabs_main_checkbox_states[0]) {
          i01.moveHead(fih.neck, fih.rothead, fih.eyeX, fih.eyeY, fih.jaw);
        }
        if (tabs_main_checkbox_states[1]) {
          i01.moveArm("left", fih.lbicep, fih.lrotate, fih.lshoulder, fih.lomoplate);
        }
        if (tabs_main_checkbox_states[2]) {
          i01.moveArm("right", fih.rbicep, fih.rrotate, fih.rshoulder, fih.romoplate);
        }
        if (tabs_main_checkbox_states[3]) {
          i01.moveHand("left", fih.lthumb, fih.lindex, fih.lmajeure, fih.lringfinger, fih.lpinky, (double) fih.lwrist);
        }
        if (tabs_main_checkbox_states[4]) {
          i01.moveHand("right", fih.rthumb, fih.rindex, fih.rmajeure, fih.rringfinger, fih.rpinky, (double) fih.rwrist);
        }
        if (tabs_main_checkbox_states[5]) {
          i01.moveTorso(fih.topStom, fih.midStom, fih.lowStom);
        }
      } else {
        if (tabs_main_checkbox_states[0]) {
          i01.setHeadSpeed(fih.neckspeed, fih.rotheadspeed, fih.eyeXspeed, fih.eyeYspeed, fih.jawspeed);
        }
        if (tabs_main_checkbox_states[1]) {
          i01.setArmSpeed("left", fih.lbicepspeed, fih.lrotatespeed, fih.lshoulderspeed, fih.lomoplatespeed);
        }
        if (tabs_main_checkbox_states[2]) {
          i01.setArmSpeed("right", fih.rbicepspeed, fih.rrotatespeed, fih.rshoulderspeed, fih.romoplatespeed);
        }
        if (tabs_main_checkbox_states[3]) {
          i01.setHandSpeed("left", fih.lthumbspeed, fih.lindexspeed, fih.lmajeurespeed, fih.lringfingerspeed, fih.lpinkyspeed, fih.lwristspeed);
        }
        if (tabs_main_checkbox_states[4]) {
          i01.setHandSpeed("right", fih.rthumbspeed, fih.rindexspeed, fih.rmajeurespeed, fih.rringfingerspeed, fih.rpinkyspeed, fih.rwristspeed);
        }
        if (tabs_main_checkbox_states[5]) {
          i01.setTorsoSpeed(fih.topStomspeed, fih.midStomspeed, fih.lowStomspeed);
        }
      }
    }
  }

  public void frame_up(JList framelist) {
    // Move this frame one up on the framelist (button bottom-right)
    int pos = framelist.getSelectedIndex();

    if (pos != -1) {
      FrameItemHolder fih = frameitemholder.remove(pos);
      frameitemholder.add(pos - 1, fih);

      framelistact(framelist);
    }
  }

  public void frame_update(JList framelist, JTextField frame_add_textfield, JTextField frame_addsleep_textfield, JTextField frame_addspeech_textfield) {
    // Update this frame on the framelist (button bottom-right)

    int pos = framelist.getSelectedIndex();

    if (pos != -1) {
      FrameItemHolder fih = new FrameItemHolder();

      // sleep || speech || servo movement || speed setting
      if (frameitemholder.get(pos).sleep != -1) {
        fih.sleep = Integer.parseInt(frame_addsleep_textfield.getText());
        fih.speech = null;
        fih.name = null;
      } else if (frameitemholder.get(pos).speech != null) {
        fih.sleep = -1;
        fih.speech = frame_addspeech_textfield.getText();
        fih.name = null;
      } else if (frameitemholder.get(pos).name != null) {
        fih.rthumb = servoitemholder[0][0].sli.getValue();
        fih.rindex = servoitemholder[0][1].sli.getValue();
        fih.rmajeure = servoitemholder[0][2].sli.getValue();
        fih.rringfinger = servoitemholder[0][3].sli.getValue();
        fih.rpinky = servoitemholder[0][4].sli.getValue();
        fih.rwrist = servoitemholder[0][5].sli.getValue();

        fih.rbicep = servoitemholder[1][0].sli.getValue();
        fih.rrotate = servoitemholder[1][1].sli.getValue();
        fih.rshoulder = servoitemholder[1][2].sli.getValue();
        fih.romoplate = servoitemholder[1][3].sli.getValue();

        fih.lthumb = servoitemholder[2][0].sli.getValue();
        fih.lindex = servoitemholder[2][1].sli.getValue();
        fih.lmajeure = servoitemholder[2][2].sli.getValue();
        fih.lringfinger = servoitemholder[2][3].sli.getValue();
        fih.lpinky = servoitemholder[2][4].sli.getValue();
        fih.lwrist = servoitemholder[2][5].sli.getValue();

        fih.lbicep = servoitemholder[3][0].sli.getValue();
        fih.lrotate = servoitemholder[3][1].sli.getValue();
        fih.lshoulder = servoitemholder[3][2].sli.getValue();
        fih.lomoplate = servoitemholder[3][3].sli.getValue();

        fih.neck = servoitemholder[4][0].sli.getValue();
        fih.rothead = servoitemholder[4][1].sli.getValue();
        fih.eyeX = servoitemholder[4][2].sli.getValue();
        fih.eyeY = servoitemholder[4][3].sli.getValue();
        fih.jaw = servoitemholder[4][4].sli.getValue();

        fih.topStom = servoitemholder[5][0].sli.getValue();
        fih.midStom = servoitemholder[5][1].sli.getValue();
        fih.lowStom = servoitemholder[5][2].sli.getValue();

        fih.sleep = -1;
        fih.speech = null;
        fih.name = frame_add_textfield.getText();
      } else {
        fih.rthumbspeed = Float.parseFloat(servoitemholder[0][0].spe.getText());
        fih.rindexspeed = Float.parseFloat(servoitemholder[0][1].spe.getText());
        fih.rmajeurespeed = Float.parseFloat(servoitemholder[0][2].spe.getText());
        fih.rringfingerspeed = Float.parseFloat(servoitemholder[0][3].spe.getText());
        fih.rpinkyspeed = Float.parseFloat(servoitemholder[0][4].spe.getText());
        fih.rwristspeed = Float.parseFloat(servoitemholder[0][5].spe.getText());

        fih.rbicepspeed = Float.parseFloat(servoitemholder[1][0].spe.getText());
        fih.rrotatespeed = Float.parseFloat(servoitemholder[1][1].spe.getText());
        fih.rshoulderspeed = Float.parseFloat(servoitemholder[1][2].spe.getText());
        fih.romoplatespeed = Float.parseFloat(servoitemholder[1][3].spe.getText());

        fih.lthumbspeed = Float.parseFloat(servoitemholder[2][0].spe.getText());
        fih.lindexspeed = Float.parseFloat(servoitemholder[2][1].spe.getText());
        fih.lmajeurespeed = Float.parseFloat(servoitemholder[2][2].spe.getText());
        fih.lringfingerspeed = Float.parseFloat(servoitemholder[2][3].spe.getText());
        fih.lpinkyspeed = Float.parseFloat(servoitemholder[2][4].spe.getText());
        fih.lwristspeed = Float.parseFloat(servoitemholder[2][5].spe.getText());

        fih.lbicepspeed = Float.parseFloat(servoitemholder[3][0].spe.getText());
        fih.lrotatespeed = Float.parseFloat(servoitemholder[3][1].spe.getText());
        fih.lshoulderspeed = Float.parseFloat(servoitemholder[3][2].spe.getText());
        fih.lomoplatespeed = Float.parseFloat(servoitemholder[3][3].spe.getText());

        fih.neckspeed = Float.parseFloat(servoitemholder[4][0].spe.getText());
        fih.rotheadspeed = Float.parseFloat(servoitemholder[4][1].spe.getText());
        fih.eyeXspeed = Float.parseFloat(servoitemholder[4][2].spe.getText());
        fih.eyeYspeed = Float.parseFloat(servoitemholder[4][3].spe.getText());
        fih.jawspeed = Float.parseFloat(servoitemholder[4][4].spe.getText());

        fih.topStomspeed = Float.parseFloat(servoitemholder[5][0].spe.getText());
        fih.midStomspeed = Float.parseFloat(servoitemholder[5][1].spe.getText());
        fih.lowStomspeed = Float.parseFloat(servoitemholder[5][2].spe.getText());

        fih.sleep = -1;
        fih.speech = null;
        fih.name = null;
      }
      frameitemholder.set(pos, fih);

      framelistact(framelist);
    }
  }

  public void framelistact(JList framelist) {
    // Re-Build the framelist
    String[] listdata = new String[frameitemholder.size()];

    for (int i = 0; i < frameitemholder.size(); i++) {
      FrameItemHolder fih = frameitemholder.get(i);

      String displaytext = "";

      // servo movement || sleep || speech || speed setting
      if (fih.sleep != -1) {
        displaytext = "SLEEP   " + fih.sleep;
      } else if (fih.speech != null) {
        displaytext = "SPEECH   " + fih.speech;
      } else if (fih.name != null) {
        String displaytext1 = "";
        String displaytext2 = "";
        String displaytext3 = "";
        String displaytext4 = "";
        String displaytext5 = "";
        String displaytext6 = "";
        if (tabs_main_checkbox_states[0]) {
          displaytext1 = fih.rthumb + " " + fih.rindex + " " + fih.rmajeure + " " + fih.rringfinger + " " + fih.rpinky + " " + fih.rwrist;
        }
        if (tabs_main_checkbox_states[1]) {
          displaytext2 = fih.rbicep + " " + fih.rrotate + " " + fih.rshoulder + " " + fih.romoplate;
        }
        if (tabs_main_checkbox_states[2]) {
          displaytext3 = fih.lthumb + " " + fih.lindex + " " + fih.lmajeure + " " + fih.lringfinger + " " + fih.lpinky + " " + fih.lwrist;
        }
        if (tabs_main_checkbox_states[3]) {
          displaytext4 = fih.lbicep + " " + fih.lrotate + " " + fih.lshoulder + " " + fih.lomoplate;
        }
        if (tabs_main_checkbox_states[4]) {
          displaytext5 = fih.neck + " " + fih.rothead + " " + fih.eyeX + " " + fih.eyeY + " " + fih.jaw;
        }
        if (tabs_main_checkbox_states[5]) {
          displaytext6 = fih.topStom + " " + fih.midStom + " " + fih.lowStom;
        }
        displaytext = fih.name + ": " + displaytext1 + " | " + displaytext2 + " | " + displaytext3 + " | " + displaytext4 + " | " + displaytext5 + " | " + displaytext6;
      } else {
        String displaytext1 = "";
        String displaytext2 = "";
        String displaytext3 = "";
        String displaytext4 = "";
        String displaytext5 = "";
        String displaytext6 = "";
        if (tabs_main_checkbox_states[0]) {
          displaytext1 = fih.rthumbspeed + " " + fih.rindexspeed + " " + fih.rmajeurespeed + " " + fih.rringfingerspeed + " " + fih.rpinkyspeed + " " + fih.rwristspeed;
        }
        if (tabs_main_checkbox_states[1]) {
          displaytext2 = fih.rbicepspeed + " " + fih.rrotatespeed + " " + fih.rshoulderspeed + " " + fih.romoplatespeed;
        }
        if (tabs_main_checkbox_states[2]) {
          displaytext3 = fih.lthumbspeed + " " + fih.lindexspeed + " " + fih.lmajeurespeed + " " + fih.lringfingerspeed + " " + fih.lpinkyspeed + " " + fih.lwristspeed;
        }
        if (tabs_main_checkbox_states[3]) {
          displaytext4 = fih.lbicepspeed + " " + fih.lrotatespeed + " " + fih.lshoulderspeed + " " + fih.lomoplatespeed;
        }
        if (tabs_main_checkbox_states[4]) {
          displaytext5 = fih.neckspeed + " " + fih.rotheadspeed + " " + fih.eyeXspeed + " " + fih.eyeYspeed + " " + fih.jawspeed;
        }
        if (tabs_main_checkbox_states[5]) {
          displaytext6 = fih.topStomspeed + " " + fih.midStomspeed + " " + fih.lowStomspeed;
        }
        displaytext = "SPEED   " + displaytext1 + " | " + displaytext2 + " | " + displaytext3 + " | " + displaytext4 + " | " + displaytext5 + " | " + displaytext6;
      }
      listdata[i] = displaytext;
    }

    framelist.setListData(listdata);
  }

  public void parsescript(JList control_list) {
    pythonitemholder.clear();

    if (true) {
      String pscript = pythonscript;
      String[] pscriptsplit = pscript.split("\n");

      // get the name of the InMoov-reference
      for (String line : pscriptsplit) {
        if (line.contains(" = Runtime.createAndStart(") || line.contains("Runtime.start(")) {
          if (line.contains(", \"InMoov\")")) {
            pythonname = line.substring(0, line.indexOf(" = "));
            referencename = line.substring(line.indexOf("(\"") + 2, line.indexOf("\", \"InMoov\")"));
          }

        }
      }

      PythonItemHolder pih = null;
      boolean keepgoing = true;
      int pos = 0;
      while (keepgoing) {
        if (pih == null) {
          pih = new PythonItemHolder();
        }
        if (pos >= pscriptsplit.length) {
          keepgoing = false;
          break;
        }
        String line = pscriptsplit[pos];
        String linewithoutspace = line.replace(" ", "");
        if (linewithoutspace.equals("")) {
          pos++;
          continue;
        }
        if (linewithoutspace.startsWith("#")) {
          pih.code = pih.code + "\n" + line;
          pos++;
          continue;
        }
        line = line.replace("  ", "    "); // 2 -> 4
        line = line.replace("   ", "    "); // 3 -> 4
        line = line.replace("     ", "    "); // 5 -> 4
        line = line.replace("      ", "    "); // 6 -> 4

        if (!(pih.function) && !(pih.notfunction)) {
          if (line.startsWith("def")) {
            pih.function = true;
            pih.notfunction = false;
            pih.modifyable = false;
            pih.code = line;
            pos++;
          } else {
            pih.notfunction = true;
            pih.function = false;
            pih.modifyable = false;
            pih.code = line;
            pos++;
          }
        } else if (pih.function && !(pih.notfunction)) {
          if (line.startsWith("    ")) {
            pih.code = pih.code + "\n" + line;
            pos++;
          } else {
            pythonitemholder.add(pih);
            pih = null;
          }
        } else if (!(pih.function) && pih.notfunction) {
          if (!(line.startsWith("def"))) {
            pih.code = pih.code + "\n" + line;
            pos++;
          } else {
            pythonitemholder.add(pih);
            pih = null;
          }
        } else {
          // it should never end here ...
          // .function & .notfunction true ...
          // would be wrong ...
        }
      }
      pythonitemholder.add(pih);
    }

    if (true) {
      ArrayList<PythonItemHolder> pythonitemholder1 = pythonitemholder;
      pythonitemholder = new ArrayList<PythonItemHolder>();
      for (PythonItemHolder pih : pythonitemholder1) {
        if (pih.function && !(pih.notfunction)) {
          String code = pih.code;
          String[] codesplit = code.split("\n");
          String code2 = "";
          for (String line : codesplit) {
            line = line.replace(" ", "");
            if (line.startsWith("def")) {
              line = "";
            } else if (line.startsWith("sleep")) {
              line = "";
            } else if (line.startsWith(pythonname)) {
              if (line.startsWith(pythonname + ".move")) {
                if (line.startsWith(pythonname + ".moveHead")) {
                  line = "";
                } else if (line.startsWith(pythonname + ".moveHand")) {
                  line = "";
                } else if (line.startsWith(pythonname + ".moveArm")) {
                  line = "";
                } else if (line.startsWith(pythonname + ".moveTorso")) {
                  line = "";
                }
              } else if (line.startsWith(pythonname + ".set")) {
                if (line.startsWith(pythonname + ".setHeadSpeed")) {
                  line = "";
                } else if (line.startsWith(pythonname + ".setHandSpeed")) {
                  line = "";
                } else if (line.startsWith(pythonname + ".setArmSpeed")) {
                  line = "";
                } else if (line.startsWith(pythonname + ".setTorsoSpeed")) {
                  line = "";
                }
              } else if (line.startsWith(pythonname + ".mouth.speak")) {
                line = "";
              }
            }
            code2 = code2 + line;
          }
          if (code2.length() > 0) {
            pih.modifyable = false;
          } else {
            pih.modifyable = true;
          }
        } else if (!(pih.function) && pih.notfunction) {
          pih.modifyable = false;
        } else {
          // shouldn't get here
          // both true or both false
          // wrong
        }
        pythonitemholder.add(pih);
      }
    }
    controllistact(control_list);
  }

  public void servoitemholder_set_sih1(int i1, ServoItemHolder[] sih1) {
    // Setting references
    servoitemholder[i1] = sih1;
  }

  public void servoitemholder_slider_changed(int t1, int t2) {
    // One slider were adjusted
    servoitemholder[t1][t2].akt.setText(servoitemholder[t1][t2].sli.getValue() + "");
    // Move the Servos in "Real-Time"
    if (moverealtime && i01 != null) {
      FrameItemHolder fih = new FrameItemHolder();

      fih.rthumb = servoitemholder[0][0].sli.getValue();
      fih.rindex = servoitemholder[0][1].sli.getValue();
      fih.rmajeure = servoitemholder[0][2].sli.getValue();
      fih.rringfinger = servoitemholder[0][3].sli.getValue();
      fih.rpinky = servoitemholder[0][4].sli.getValue();
      fih.rwrist = servoitemholder[0][5].sli.getValue();

      fih.rbicep = servoitemholder[1][0].sli.getValue();
      fih.rrotate = servoitemholder[1][1].sli.getValue();
      fih.rshoulder = servoitemholder[1][2].sli.getValue();
      fih.romoplate = servoitemholder[1][3].sli.getValue();

      fih.lthumb = servoitemholder[2][0].sli.getValue();
      fih.lindex = servoitemholder[2][1].sli.getValue();
      fih.lmajeure = servoitemholder[2][2].sli.getValue();
      fih.lringfinger = servoitemholder[2][3].sli.getValue();
      fih.lpinky = servoitemholder[2][4].sli.getValue();
      fih.lwrist = servoitemholder[2][5].sli.getValue();

      fih.lbicep = servoitemholder[3][0].sli.getValue();
      fih.lrotate = servoitemholder[3][1].sli.getValue();
      fih.lshoulder = servoitemholder[3][2].sli.getValue();
      fih.lomoplate = servoitemholder[3][3].sli.getValue();

      fih.neck = servoitemholder[4][0].sli.getValue();
      fih.rothead = servoitemholder[4][1].sli.getValue();
      fih.eyeX = servoitemholder[4][2].sli.getValue();
      fih.eyeY = servoitemholder[4][3].sli.getValue();
      fih.jaw = servoitemholder[4][4].sli.getValue();

      fih.topStom = servoitemholder[5][0].sli.getValue();
      fih.midStom = servoitemholder[5][1].sli.getValue();
      fih.lowStom = servoitemholder[5][2].sli.getValue();

      if (tabs_main_checkbox_states[0]) {
        i01.moveHead(fih.neck, fih.rothead, fih.eyeX, fih.eyeY, fih.jaw);
      }
      if (tabs_main_checkbox_states[1]) {
        i01.moveArm("left", fih.lbicep, fih.lrotate, fih.lshoulder, fih.lomoplate);
      }
      if (tabs_main_checkbox_states[2]) {
        i01.moveArm("right", fih.rbicep, fih.rrotate, fih.rshoulder, fih.romoplate);
      }
      if (tabs_main_checkbox_states[3]) {
        i01.moveHand("left", fih.lthumb, fih.lindex, fih.lmajeure, fih.lringfinger, fih.lpinky, (double) fih.lwrist);
      }
      if (tabs_main_checkbox_states[4]) {
        i01.moveHand("right", fih.rthumb, fih.rindex, fih.rmajeure, fih.rringfinger, fih.rpinky, (double) fih.rwrist);
      }
      if (tabs_main_checkbox_states[5]) {
        i01.moveTorso(fih.topStom, fih.midStom, fih.lowStom);
      }
    }
  }

  public void tabs_main_checkbox_states_changed(boolean[] tabs_main_checkbox_states2) {
    // checkbox states (on the main site) (for the services) changed
    tabs_main_checkbox_states = tabs_main_checkbox_states2;
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

    ServiceType meta = new ServiceType(InMoovGestureCreator.class.getCanonicalName());
    meta.addDescription("an easier way to create gestures for InMoov");
    meta.addCategory("robot");

    return meta;
  }

}
