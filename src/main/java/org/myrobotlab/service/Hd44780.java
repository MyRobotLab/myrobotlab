package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.Hd44780Config;
import org.myrobotlab.service.config.Pcf8574Config;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.I2CControl;
import org.slf4j.Logger;

/**
 * 
 * HD44780 - Driver to play with lcd display panel Tested with HD44780 1602
 * panel, attached to pcf8574 Driver pasted from Poduzov :
 * https://github.com/Poduzov/PI4J-I2C-LCD
 * 
 * @author Moz4r
 * 
 */
public class Hd44780 extends Service {

  public final static Logger log = LoggerFactory.getLogger(Hd44780.class);

  private static final long serialVersionUID = 1L;

  protected boolean backLight;

  public final byte En = (byte) 0b00000100; // Enable bit
  protected boolean initialized = false;
  public final byte LCD_1LINE = (byte) 0x00;
  public final byte LCD_2LINE = (byte) 0x08;
  public final byte LCD_4BITMODE = (byte) 0x00;
  public final byte LCD_5x10DOTS = (byte) 0x04;
  public final byte LCD_5x8DOTS = (byte) 0x00;
  // flags for function set
  public final byte LCD_8BITMODE = (byte) 0x10;

  // flags for backlight control
  public final byte LCD_BACKLIGHT = (byte) 0x08;
  public final byte LCD_BLINKOFF = (byte) 0x00;
  public final byte LCD_BLINKON = (byte) 0x01;
  public final byte LCD_CLEARDISPLAY = (byte) 0x01;

  public final byte LCD_CURSORMOVE = (byte) 0x00;
  public final byte LCD_CURSOROFF = (byte) 0x00;
  public final byte LCD_CURSORON = (byte) 0x02;
  public final byte LCD_CURSORSHIFT = (byte) 0x10;
  public final byte LCD_DISPLAYCONTROL = (byte) 0x08;
  // flags for display/cursor shift
  public final byte LCD_DISPLAYMOVE = (byte) 0x08;

  public final byte LCD_DISPLAYOFF = (byte) 0x00;
  // flags for display on/off control
  public final byte LCD_DISPLAYON = (byte) 0x04;
  public final byte LCD_ENTRYLEFT = (byte) 0x02;
  public final byte LCD_ENTRYMODESET = (byte) 0x04;

  // flags for display entry mode
  public final byte LCD_ENTRYRIGHT = (byte) 0x00;
  public final byte LCD_ENTRYSHIFTDECREMENT = (byte) 0x00;
  public final byte LCD_ENTRYSHIFTINCREMENT = (byte) 0x01;
  public final byte LCD_FUNCTIONSET = (byte) 0x20;
  public final byte LCD_MOVELEFT = (byte) 0x00;
  public final byte LCD_MOVERIGHT = (byte) 0x04;

  public final byte LCD_NOBACKLIGHT = (byte) 0x00;
  public final byte LCD_RETURNHOME = (byte) 0x02;

  public final byte LCD_SETCGRAMADDR = (byte) 0x40;
  public final byte LCD_SETDDRAMADDR = (byte) 0x80;
  /**
   * FIXME - changed to I2CController !!!!
   */
  transient private Pcf8574 pcf;

  protected String pcfName;

  protected boolean isAttached = false;

  public final byte Rs = (byte) 0b00000001; // Register select bit

  public final byte Rw = (byte) 0b00000010; // Read/Write bit

  protected Map<Integer, String> screenContent = new HashMap<Integer, String>();
  
  int lcdWriteDelayMs = 0;

  public Hd44780(String n, String id) {
    super(n, id);
    // I think "only" PCF is supported not all I2CControls
    registerForInterfaceChange(I2CControl.class);
  }
  
  /**
   * sets the delay between character writes
   * @param delay
   * @return
   */
  public int setDelay(int delay) {
    lcdWriteDelayMs = delay;
    return delay;
  }
  
  /**
   * current delay between in millis between character writes
   * @return
   */
  public int getDelay() {
    return lcdWriteDelayMs;
  }

  @Override
  public void attach(String name) {
    ServiceInterface si = Runtime.getService(name);
    if (si instanceof Pcf8574) {
      attachPcf8574((Pcf8574) si);
      return;
    } else {
      error("%s does not know how to attach to %s of type %s", getName(), name, si.getSimpleName());
    }
  }

  @Override
  public void attach(Attachable service) throws Exception {
    attach(service.getName());
  }

  @Override
  public void detach(String serviceName) {
    if (serviceName != null && pcf != null && serviceName.equals(pcf.getName())) {
      pcf = null;
      isAttached = false;
      pcfName = null;
      broadcastState();
    }
  }

  @Override
  public void detach() {
    if (pcf != null) {
      detach(pcf.getName());
    }
  }

  @Override
  public void detach(Attachable attachable) {
    detach(attachable.getName());
  }

  public void attachPcf8574(Pcf8574 pcf8574) {
    pcf = pcf8574;
    isAttached = true;
    pcfName = pcf.getName();
    broadcastState();
  }

  public void reset() {
    init();
  }
  

  /**
   * clear lcd and set to home
   */
  public void clear() {
    if (!initialized) {
      init();
    }
    lcdWrite((byte) LCD_CLEARDISPLAY);
    lcdWrite((byte) LCD_RETURNHOME);
    screenContent.clear();
    log.info("lcd cleared");
  }

  /**
   * Display string on LCD, by line
   * 
   * @param string
   *          s
   * @param line
   *          l
   * 
   */
  public void display(String string, int line) {
    if (!initialized) {
      init();
    }
    screenContent.put(line, string);
    broadcastState();
    switch (line) {
      case 0:
        lcdWrite((byte) 0x80);
        break;
      case 1:
        lcdWrite((byte) 0xC0);
        break;
      case 2:
        lcdWrite((byte) 0x94);
        break;
      case 3:
        lcdWrite((byte) 0xD4);
        break;
      default:
        error("line %d is invalid, valid line values are 0 - 3");
    }

    for (int i = 0; i < string.length(); i++) {
      lcdWrite((byte) string.charAt(i), Rs);
    }
  }

  public boolean getBackLight() {
    return backLight;
  }

  /**
   * INIT LCD panel
   */
  public void init() {

    if (!isAttached) {
      warn("must be attached to initialize");
    }
    log.info("Init I2C Display");
    lcdWrite((byte) 0x03);
    lcdWrite((byte) 0x03);
    lcdWrite((byte) 0x03);
    lcdWrite((byte) 0x02);
    lcdWrite((byte) (LCD_FUNCTIONSET | LCD_2LINE | LCD_5x8DOTS | LCD_4BITMODE));
    lcdWrite((byte) (LCD_DISPLAYCONTROL | LCD_DISPLAYON));
    lcdWrite((byte) (LCD_CLEARDISPLAY));
    lcdWrite((byte) (LCD_ENTRYMODESET | LCD_ENTRYLEFT));
    initialized = true;
  }

  private void lcdStrobe(byte data) {
    pcf.writeRegister((byte) (data | En | LCD_BACKLIGHT));
    pcf.writeRegister((byte) ((data & ~En) | LCD_BACKLIGHT));
  }

  // write a command to lcd
  private void lcdWrite(byte cmd) {
    lcdWrite(cmd, (byte) 0);
  }
  
  synchronized private void lcdWrite(byte cmd, byte mode) {
    lcdWriteFourBits((byte) (mode | (cmd & 0xF0)));
    lcdWriteFourBits((byte) (mode | ((cmd << 4) & 0xF0)));
    sleep(lcdWriteDelayMs); //  heh fun typing effect
  }

  private void lcdWriteFourBits(byte data) {

    pcf.writeRegister((byte) (data | LCD_BACKLIGHT));
    lcdStrobe(data);
  }

  @Override
  public void preShutdown() {
    if (isAttached) {
      clear();
      setBackLight(false);
    }
    super.stopService();
  }

  /**
   * Turn ON/OFF LCD backlight
   * 
   * @param status
   *          s
   * 
   */
  public void setBackLight(boolean status) {
    if (status == true) {
      writeRegister(LCD_BACKLIGHT);
    } else {
      writeRegister(LCD_NOBACKLIGHT);
    }
    backLight = status;
    log.info("LCD backlight set to {}", status);
    broadcastState();
  }

  /**
   * Send byte to PCF controller
   * 
   * @param cmd
   *          c
   * 
   */
  public void writeRegister(byte cmd) {
    if (isReady() && pcf != null) {
      pcf.writeRegister(cmd);
    } else {
      log.error("LCD is not ready / attached !");
    }
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);
      Platform.setVirtual(true);

      Runtime.start("webgui", "WebGui");
      Pcf8574 pcf = (Pcf8574) Runtime.start("pcf8574t", "Pcf8574");
      Runtime.start("lcd", "Hd44780");
      Arduino mega = (Arduino) Runtime.start("mega", "Arduino");

      boolean done = true;
      if (done) {
        return;
      }

      mega.connect("COM4");

      pcf.setBus("1");
      pcf.setAddress("0x27");
      pcf.attach(mega);

      Hd44780 lcd = (Hd44780) Runtime.start("lcd", "Hd44780");

      lcd.attach(pcf);
      lcd.init();
      lcd.setBackLight(true);
      lcd.display("Spot is ready !", 1);
      lcd.display("* MyRobotLab *", 2);

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }
  
  @Override
  public ServiceConfig getConfig() {
    Hd44780Config config = new Hd44780Config();
    if (pcfName != null) {      
      config.controller = pcfName;
    }
    config.backlight = backLight;
    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    Hd44780Config config = (Hd44780Config) c;
    
    if (config.controller != null) {
      try {
        attach(config.controller);
      } catch(Exception e) {
        error(e);
      }
    }
    
    if (config.delay != null) {
      setDelay(config.delay);
    }
    
    if (pcf != null && config.backlight != null && config.backlight) {
      setBackLight(config.backlight);
    }
    return c;
  }
}