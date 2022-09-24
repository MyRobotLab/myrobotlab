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

  public final byte Rs = (byte) 0b00000001; // Register select bit
  public final byte Rw = (byte) 0b00000010; // Read/Write bit
  public final byte En = (byte) 0b00000100; // Enable/Strobe bit
  public final byte Bl = (byte) 0b00001000; // Backlight bit
  public final byte DB = (byte) 0b11110000; // Data Bits
  public final byte BF = (byte) 0b10000000; // Busy Flag
  /**
  * At first startup this is set to false.
  * Call the init() method to set it to true.
  */
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
  /*
   * The 16x6 LCD display module using the HD44780 controller chip have the following terminals.
   * Pin  PCF8574 Function
   * VSS          Ground or 0V
   * VDD          5V dc power
   * V0   Trimpot Contrast Control
   * RS   P0      Row Select
   * RW   P1      Read/Write
   * E    P2      Execute / Strobe / Clock Data in out Pulse high for 230nS
   * D0   NC      Data bit 0
   * D1   NC      Data bit 1
   * D2   NC      Data bit 2
   * D3   NC      Data bit 3
   * D4   P4      Data bit 4
   * D5   P5      Data bit 5
   * D6   P6      Data bit 6
   * D7   P7      Data bit 7
   * A    VCC     Back Light Anode
   * K    P3      Back Light Cathode
   * 
   * In theory, we could connect this to an Arduino.
   * Using 4 bit transfer mode pins D0 - D3 are not used
   * A nibble is 4 bits, the high nibble is sent first then the low nibble
   */

  /**
   * FIXME - changed to I2CController !!!!
   */
  transient private Pcf8574 pcf;

  protected String pcfName;

  protected boolean isAttached = false;


  protected Map<Integer, String> screenContent = new HashMap<Integer, String>();
  
  public Hd44780(String n, String id) {
    super(n, id);
    // I think "only" PCF is supported not all I2CControls
    registerForInterfaceChange(I2CControl.class);
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

  /**
   * Display string on LCD, by line
   * 
   * @param string
   *          String to be sent to the display.
   *          Note:
   *            On the 2 line x 16 display, only the first 16 characters will be used
   *            On the 4 line x 20 display, when writing to Line 0, the 21st character will appear on line 2
   *            When writing to line 1, the 21st character will appear on line 3
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
      setDDRAMaddress((byte) 0x00);
        break;
      case 1:
      setDDRAMaddress((byte) 0x40);
        break;
      case 2:
      setDDRAMaddress((byte) 0x14);
        break;
      case 3:
      setDDRAMaddress((byte) 0x3C);
        break;
      default:
        error("line %d is invalid, valid line values are 0 - 3");
    }
    lcdWriteDataString(string);
  }

  /**
   * Clear lcd and set to home.
   */
  public void clear() {
    if (!initialized) {
      init();
    }
    clearDisplay();
    returnHome();
    screenContent.clear();
    log.info("lcd cleared");
  }

  /**
   * Reset the HD44780 by performing an init()
   */
  public void reset() {
    init();
  }
  
  /**
   * returns the current state of the back light.
   * 
   * @return 
   *        true for on.
   *        false for off.
   */
  public boolean getBackLight() {
    return backLight;
  }

  /**
   * Turn ON/OFF LCD backlight
   * 
   * @param status
   *        true = Backlight is on.
   *        flase = Backlight off.
   */
  public void setBackLight(boolean status) {
    backLight = status;
    lcdWriteCmd((byte) 0);
    log.info("LCD backlight set to {}", status);
    broadcastState();
  }

  /**
   * INIT LCD panel.
   * This will clear the display and configure it ready to write to the screen
   */
  public void init() {

    if (!isAttached) {
      warn("must be attached to initialize");
    }else{
      log.info("Init I2C Display");
      functionSet(true, false); //Set the function Control.
      clearDisplay(); //Clear the Display and set DDRAM address 0.
      returnHome(); //Set DDRAM address 0 and return display home.
      displayControl(true, true, false); //Set the Display Control to on, Cursor On, Not blinking.
      cursorDisplayShift(false, false); // Set the cursor to move to the left
      entryModeSet(true, false); //Set the Entry Mode Control.
      initialized = true;
    }
  }

  /**
   * Function set set the data length to either 4 or 8 bit, the number of lines to use and the size of the font.
   * Because we are using the PCF8574, the data length will always be 4.
   * We MUST call this first during the Init to set the data length.
   * In most cases we will use 4 lines.
   * And 5x8 dots for the font.
   * @param lines
   *        true = 2 lines (this applies for 4 line displays as well).
   *        false = 1 line.
   * @param font
   *        true = 5 x 10 dots.
   *        false = 5 x 8 dots.
   */
  public void functionSet(boolean lines, boolean font) {
    byte n = 0b00001000;
    byte f = 0b00000100;
    if (!lines) {n = 0;}
    if (!font) {f = 0;}
    lcdWriteCmd((byte)(0b00100000 | n | f));
  }

  /**
   * Moves cursor and shifts display without changing DDRAM contents
   * @param displayShift 
   *        true = Display moves. 
   *        false = Cursor moves.
   * @param rightLeft 
   *        Only used when the display set to move
   *        true = Shift to the right.
   *        false = Shift to the left.
   */
  public void cursorDisplayShift(boolean displayShift, boolean rightLeft) {
    byte Sc = 0b00001000;
    byte Rl = 0b00000100;
    if (!displayShift) {Sc = 0;}
    if (!rightLeft) {Rl = 0;}
    lcdWriteCmd((byte)(0b00010000 | Sc | Rl));
  }

  /**
   * Display on/off control.
   * @param display 
   *        true the display is on. 
   *        false the display is off.
   * @param cursor 
   *        true the cursor is on. 
   *        false the cursor is off.
   * @param blink 
   *        true the cursor if enabled blinks.
   *        false the cursor if enabled does not blink.
   */
  public void displayControl(boolean display, boolean cursor, boolean blink) {
    byte De = 0b00000100;
    byte Ce = 0b00000010;
    byte Cb = 0b00000001;
    if (!display) {De = 0;}
    if (!cursor) {Ce = 0;}
    if(!blink) {Cb = 0;}
    lcdWriteCmd((byte)(0b00001000 | De | Ce | Cb));
  }

  /**
   * Sets cursor move direction and specifies display shift.
   * These operations are performed during data write and read.
   * 
   * @param incDec
   *        true = Increment address counter.
   *        false = Decrement address counter.
   * @param s
   *        Accompanies display shift.  
   *        The datasheet wasn't that helpfull in this case.
   */
  public void entryModeSet(boolean incDec, boolean s) {
    byte Id = 0b00000010;
    byte Shift = 0b00000001;
    if(!incDec) { Id = 0;}
    if (!s) { Shift = 0;}
    lcdWriteCmd((byte)(0b00000100 | Id | Shift));
  }

  /**
   * Sets DDRAM address 0 in the address counter.
   * Also returns display from being shifted to original position.
   * DDRAM contents remain unchanged.
   */
  public void returnHome() {
    lcdWriteCmd((byte)0b00000010);
  }

  /**
   * Clears entire display and sets DDRAM address 0 in the address counter.
   */
  public void clearDisplay() {
    lcdWriteCmd((byte)0b00000001);
  }

  /**
   * We can place the cursor anywhere in the range of 0 - 79 within the LCD memory.
   * Each memory location corresponds to a position on the display
   * For both 2 x 16 displays and the 4 x 20 displays:
   *        Address 0 is the left most character on the first line.
   *        Address 40 is the left most character on the second line.
   * For the 4 x 20 dispalys:
   *        Address 20 is the left most character on the third line.
   *        Address 60 is the left most character on the fourth line.
   * A continous series of writes to the DDRAM will wrap from the end of the first line to the start of the thrid line, then back to the second line and finally the fourth line.
   * @param address
   */
  public void setDDRAMaddress(int address) {
      if (address < 80){ // Make sure the address is in a valid range
        lcdWriteCmd((byte) (address | 0b10000000));
      } else {
        error("%d Outside allowed range 0 - 79", address);
      }
  }

  /**
   * Write a string of charaters to the data register.
   * @param string must be less than 80 charaters long.
   */
  public void lcdWriteDataString(String string){
    for (int i = 0; i < string.length(); i++) {
      lcdWriteData((byte) string.charAt(i));
    }
  }

  /**
   * This method writes a byte to the data register.
   * Make sure you have previously set up the address you want the data stored in using the lcdWriteCmd(cmd).
   * A data write typically takes 37uS, well under the 200uS to write any command or data via the I2C bus.
   * @param data
   */
  private void lcdWriteData(byte data) {
    byte Blight = 0;
    if (backLight == true) {
      Blight = Bl;
    }
    byte hdata = (byte)(data & 0xF0);
    byte ldata = (byte)((data << 4) & 0xF0);
    if (isReady() && pcf != null) {
      pcf.writeRegister((byte) (0b00000101 | Blight | hdata)); // Rs | ~Rw | En
      pcf.writeRegister((byte) (0b00000001 | Blight | hdata)); // Rs | ~Rw | ~En
      pcf.writeRegister((byte) (0b00000101 | Blight | ldata)); // Rs | ~Rw | En
      pcf.writeRegister((byte) (0b00000001 | Blight | ldata)); // Rs | ~Rw | ~En
    } else {
      log.error("LCD is not ready / attached !");
    }
  }

  /**
   * This method will write the cmd value to the instruction register
   * then wait until it is ready for the next instruction.
   * Most commands are pretty quick at around 37uS, but there is the odd one at 1.52mS, 1,520uS
   * most I2C read or writes are going to be on the order 200uS assuming it gets the buss without delay.
   * Because of the 4 bit operation with the strobe, we will always have 6 read/writes before we can get the Busy Flag.
   * 
   * @param cmd
   */
  private void lcdWriteCmd(byte cmd) {
    byte Blight = 0;
    byte hresult = 0;
    byte lresult = 0;
    byte result = BF;
    if (backLight == true) {
      Blight = Bl;
    }
    byte hcmd = (byte)(cmd & 0xF0);
    byte lcmd = (byte)((cmd << 4) & 0xF0);
    if (isReady() && pcf != null) {
      if (cmd ==0) {
        pcf.writeRegister((byte) (0b00000100 | Blight | hcmd)); // ~Rs | ~Rw | En
        pcf.writeRegister((byte) (0b00000000 | Blight | hcmd)); // ~Rs | ~Rw | ~En
        pcf.writeRegister((byte) (0b00000100 | Blight | lcmd)); //  ~Rs | ~Rw | En 
        pcf.writeRegister((byte) (0b00000000 | Blight | lcmd)); // ~Rs | ~Rw | ~En
        // repeat the read loop until the Busy Flag is set to 0
        do{
          pcf.writeRegister((byte) (0x00000110 | Blight | 0xF0)); // ~Rs | Rw | En
          hresult = (byte) pcf.readRegister();
          pcf.writeRegister((byte) (0x00000010 | Blight | 0xF0)); // ~Rs | Rw | ~En 
          pcf.writeRegister((byte) (0x00000110 | Blight | 0xF0)); // ~Rs | Rw | En
          lresult = (byte) pcf.readRegister();
          pcf.writeRegister((byte) (0x00000010 | Blight | 0xF0)); // ~Rs | Rw | ~En 
          result = (byte)((hresult & 0xF0) | ((lresult & 0xF0) >> 4));
        }while ((result & BF) > 0);
      } else {
        pcf.writeRegister((byte) (0b00000000 | Blight));  // we are not sending any commands to the HD44780 so we don't need to check the busy flag.
      }
    } else {
      log.error("LCD is not ready / attached !");
    }
  }

  @Override
  public void preShutdown() {
    if (isAttached) {
      clear();
      displayControl(false, false, false);
      setBackLight(false);
    }
    super.stopService();
  }

  /**
   * Send byte to PCF controller
   * 
   * @param cmd
   *          c
   * 
   */
  // Why are we exposing the writeRegister publicly here.
  // This is a dangerous method to the HD44780 as anything written here can destabilize it.
  // not yet removed but should be depreciated or removed if not being used.
  @Deprecated // use lcdWriteCmd(byte cmd) or lcdWriteData(byte data)
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

      pcf.setBus("0");
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
  
  /**
   * Load the config into memory
   */
  @Override
  public ServiceConfig getConfig() {
    Hd44780Config config = new Hd44780Config();
    if (pcfName != null) {      
      config.controller = pcfName;
    }
    config.backlight = backLight;
    return config;
  }

  /**
   * Applies the config to the service attaching to the PCF8574 if it exists.
   */
  public ServiceConfig apply(ServiceConfig c) {
    Hd44780Config config = (Hd44780Config) c;
    
    if (config.controller != null) {
      try {
        attach(config.controller);
      } catch(Exception e) {
        error(e);
      }
    }
   
    if (pcf != null && config.backlight != null && config.backlight) {
      setBackLight(config.backlight);
    }
    return c;
  }
}