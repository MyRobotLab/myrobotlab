package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * 
 * HD44780 - Driver to play with lcd display panel
 * Tested with HD44780 1602 panel, attached to pcf8574
 * Driver pasted from Poduzov : https://github.com/Poduzov/PI4J-I2C-LCD
 * 
 * @author Moz4r
 * 
 */
public class Hd44780 extends Service {

  public Hd44780(String n, String id) {
    super(n, id);
    setReady(false);
  }

  public final static Logger log = LoggerFactory.getLogger(Hd44780.class);
  private static final long serialVersionUID = 1L;

  private Pcf8574 pcf8574;

  private final byte LCD_CLEARDISPLAY = (byte) 0x01;
  private final byte LCD_RETURNHOME = (byte) 0x02;
  private final byte LCD_ENTRYMODESET = (byte) 0x04;
  private final byte LCD_DISPLAYCONTROL = (byte) 0x08;
  private final byte LCD_CURSORSHIFT = (byte) 0x10;
  private final byte LCD_FUNCTIONSET = (byte) 0x20;
  private final byte LCD_SETCGRAMADDR = (byte) 0x40;
  private final byte LCD_SETDDRAMADDR = (byte) 0x80;

  // flags for display entry mode 
  private final byte LCD_ENTRYRIGHT = (byte) 0x00;
  private final byte LCD_ENTRYLEFT = (byte) 0x02;
  private final byte LCD_ENTRYSHIFTINCREMENT = (byte) 0x01;
  private final byte LCD_ENTRYSHIFTDECREMENT = (byte) 0x00;

  // flags for display on/off control
  private final byte LCD_DISPLAYON = (byte) 0x04;
  private final byte LCD_DISPLAYOFF = (byte) 0x00;
  private final byte LCD_CURSORON = (byte) 0x02;
  private final byte LCD_CURSOROFF = (byte) 0x00;
  private final byte LCD_BLINKON = (byte) 0x01;
  private final byte LCD_BLINKOFF = (byte) 0x00;

  // flags for display/cursor shift
  private final byte LCD_DISPLAYMOVE = (byte) 0x08;
  private final byte LCD_CURSORMOVE = (byte) 0x00;
  private final byte LCD_MOVERIGHT = (byte) 0x04;
  private final byte LCD_MOVELEFT = (byte) 0x00;

  // flags for function set
  private final byte LCD_8BITMODE = (byte) 0x10;
  private final byte LCD_4BITMODE = (byte) 0x00;
  private final byte LCD_2LINE = (byte) 0x08;
  private final byte LCD_1LINE = (byte) 0x00;
  private final byte LCD_5x10DOTS = (byte) 0x04;
  private final byte LCD_5x8DOTS = (byte) 0x00;

  // flags for backlight control
  private final byte LCD_BACKLIGHT = (byte) 0x08;
  private final byte LCD_NOBACKLIGHT = (byte) 0x00;

  private final byte En = (byte) 0b00000100; // Enable bit
  private final byte Rw = (byte) 0b00000010; // Read/Write bit
  private final byte Rs = (byte) 0b00000001; // Register select bit

  public boolean backLight;
  public Map<Integer, String> screenContent = new HashMap<Integer, String>();

  public void attach(Pcf8574 pcf8574) {
    // we need more checkup here...
    if (pcf8574.isAttached) {
      this.pcf8574 = pcf8574;
      setReady(true);
      log.info("{} attach {}", getName(), pcf8574.getName());
    } else {
      log.error("Cannot attach {} to {}", getName(), pcf8574.getName());
    }
  }

  /**
   * INIT LCD panel
   */
  public void init() {
    log.info("Init I2C Display");
    lcdWrite((byte) 0x03);
    lcdWrite((byte) 0x03);
    lcdWrite((byte) 0x03);
    lcdWrite((byte) 0x02);
    lcdWrite((byte) (LCD_FUNCTIONSET | LCD_2LINE | LCD_5x8DOTS | LCD_4BITMODE));
    lcdWrite((byte) (LCD_DISPLAYCONTROL | LCD_DISPLAYON));
    lcdWrite((byte) (LCD_CLEARDISPLAY));
    lcdWrite((byte) (LCD_ENTRYMODESET | LCD_ENTRYLEFT));
  }

  /**
   * Send byte to PCF controller
   * @param cmd
   */
  public void writeRegister(byte cmd) {
    if (isReady()) {
      pcf8574.writeRegister(cmd);
    } else {
      log.error("LCD is not ready / attached !");
    }
  }

  /**
   * Turn ON/OFF LCD backlight
   * @param status
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

  public boolean getBackLight() {
    return backLight;
  }

  /**
   * Display string on LCD, by line 
   * @param string
   * @param line
   */
  public void display(String string, int line) {
    screenContent.put(line, string);
    broadcastState();
    switch (line) {
      case 1:
        lcdWrite((byte) 0x80);
        break;
      case 2:
        lcdWrite((byte) 0xC0);
        break;
      case 3:
        lcdWrite((byte) 0x94);
        break;
      case 4:
        lcdWrite((byte) 0xD4);
        break;
    }

    for (int i = 0; i < string.length(); i++) {
      lcdWrite((byte) string.charAt(i), Rs);
    }
  }

  private void lcdStrobe(byte data) {
    pcf8574.writeRegister((byte) (data | En | LCD_BACKLIGHT));
    pcf8574.writeRegister((byte) ((data & ~En) | LCD_BACKLIGHT));
  }

  private void lcdWriteFourBits(byte data) {

    pcf8574.writeRegister((byte) (data | LCD_BACKLIGHT));
    lcdStrobe(data);
  }

  private void lcdWrite(byte cmd, byte mode) {
    lcdWriteFourBits((byte) (mode | (cmd & 0xF0)));
    lcdWriteFourBits((byte) (mode | ((cmd << 4) & 0xF0)));
  }

  // write a command to lcd
  private void lcdWrite(byte cmd) {
    lcdWrite(cmd, (byte) 0);
  }

  /**
   * clear lcd and set to home 
   */
  public void clear() {
    lcdWrite((byte) LCD_CLEARDISPLAY);
    lcdWrite((byte) LCD_RETURNHOME);
    screenContent.clear();
    log.info("LCD content cleared");
  }

  @Override
  public void preShutdown() {
    if (isReady()) {
      clear();
      setBackLight(false);
    }
    super.stopService();
  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(Hd44780.class.getCanonicalName());
    meta.addDescription("I2C LCD Display driver");
    meta.addCategory("i2c", "display");
    return meta;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    Platform.setVirtual(true);
    Pcf8574 pcf8574t = (Pcf8574) Runtime.start("pcf8574t", "Pcf8574");
    Runtime.start("gui", "SwingGui");
    Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
    mega.connect("COM4");
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    pcf8574t.attach(mega, "1", "0x27");
    Hd44780 lcd = (Hd44780) Runtime.start("lcd", "Hd44780");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(true);
    webgui.startService();
    webgui.startBrowser("http://localhost:8888/#/service/lcd");
   
    lcd.attach(pcf8574t);
    lcd.init();
    lcd.setBackLight(true);
    lcd.display("Spot is ready !", 1);
    lcd.display("* MyRobotLab *", 2);

  }
}