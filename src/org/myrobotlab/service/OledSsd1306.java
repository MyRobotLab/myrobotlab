package org.myrobotlab.service;

import static org.myrobotlab.service.data.OledSsd1306Data.FONT;
import static org.myrobotlab.service.data.OledSsd1306Data.SSD1306_128_32Data;
import static org.myrobotlab.service.data.OledSsd1306Data.SSD1306_128_64Data;
import static org.myrobotlab.service.data.OledSsd1306Data.SSD1306_96_16Data;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.slf4j.Logger;

/**
 * 
 * OledSsd1306 - This service can be used to drive a OLED display using the i2c
 * protocol It's built for the SSD1306 driver
 * 
 * @author Mats Onnerby
 * 
 *         More Info : https://www.adafruit.com/product/326 References :
 *         https://github.com/adafruit/Adafruit_SSD1306
 * 
 *         This service builds is a conversion from of the Arduino library above
 *         to Java
 * 
 *         Some OLED's are wired for SPI and need to be changed according to
 *         this instruction so that you can use the i2c protocol ( as
 *         implemented in this program )
 * 
 *         http://electronics.stackexchange.com/questions/164680/ssd1306-display
 *         -isp-connection-or-i2c-according-to-resistors
 * 
 *         NB. Some OLED's need a reset pulse at power on. It can be generated
 *         using one of the pin's on the Arduino or by adding a 100nF capacitor
 *         between the reset pin and GND, and a 10K resistor betwen the reset
 *         pin and VCC
 * 
 */
public class OledSsd1306 extends Service implements I2CControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OledSsd1306.class.getCanonicalName());

  public static final int HIGH = 0x1;
  public static final int LOW = 0x0;
  public static final int INPUT = 0x0;
  public static final int OUTPUT = 0x1;

  public List<String> controllers = new ArrayList<String>();
  public String controllerName;
  transient public I2CController controller; // Remove // has
                                             // been
  public List<String> deviceAddressList = Arrays.asList("0x3C", "0x3D");

  public String deviceAddress = "0x3C";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  // Constants for the SSD1306
  public static short SSD1306_SETCONTRAST = 0x81;
  public static short SSD1306_DISPLAYALLON_RESUME = 0xA4;
  public static short SSD1306_DISPLAYALLON = 0xA5;
  public static short SSD1306_NORMALDISPLAY = 0xA6;
  public static short SSD1306_INVERTDISPLAY = 0xA7;
  public static short SSD1306_DISPLAYOFF = 0xAE;
  public static short SSD1306_DISPLAYON = 0xAF;

  public static short SSD1306_SETDISPLAYOFFSET = 0xD3;
  public static short SSD1306_SETCOMPINS = 0xDA;

  public static short SSD1306_SETVCOMDETECT = 0xDB;

  public static short SSD1306_SETDISPLAYCLOCKDIV = 0xD5;
  public static short SSD1306_SETPRECHARGE = 0xD9;

  public static short SSD1306_SETMULTIPLEX = 0xA8;

  public static short SSD1306_SETLOWCOLUMN = 0x00;
  public static short SSD1306_SETHIGHCOLUMN = 0x10;

  public static short SSD1306_SETSTARTLINE = 0x40;

  public static short SSD1306_MEMORYMODE = 0x20;
  public static short SSD1306_COLUMNADDR = 0x21;
  public static short SSD1306_PAGEADDR = 0x22;

  public static short SSD1306_COMSCANINC = 0xC0;
  public static short SSD1306_COMSCANDEC = 0xC8;

  public static short SSD1306_SEGREMAP = 0xA0;

  public static short SSD1306_CHARGEPUMP = 0x8D;

  public static short SSD1306_EXTERNALVCC = 0x1;
  public static short SSD1306_SWITCHCAPVCC = 0x2;

  // The different types of OLED's supported
  public static int SSD1306_128_64 = 0;
  public static int SSD1306_128_32 = 1;
  public static int SSD1306_96_16 = 2;
  public int oledType = SSD1306_128_64; // Set
                                        // default
                                        // oledType
                                        // to
                                        // 128*64

  public static final int BLACK = 0;
  public static final int WHITE = 1;
  public static final int INVERSE = 2;

  // Scrolling #defines
  public static int SSD1306_ACTIVATE_SCROLL = 0x2F;
  public static int SSD1306_DEACTIVATE_SCROLL = 0x2E;
  public static int SSD1306_SET_VERTICAL_SCROLL_AREA = 0xA3;
  public static int SSD1306_RIGHT_HORIZONTAL_SCROLL = 0x26;
  public static int SSD1306_LEFT_HORIZONTAL_SCROLL = 0x27;
  public static int SSD1306_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = 0x29;
  public static int SSD1306_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL = 0x2A;

  // Buffer for the OLED
  public int SSD1306_LCDWIDTH = 128;
  public int SSD1306_LCDHEIGHT = 64;
  public int[] buffer;
  // pin
  private int vccstate; // vccstate
                        // //
                        // vccstate
  private int rotation;

  static int premask[] = { 0x00, 0x80, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC, 0xFE };
  static int postmask[] = { 0x00, 0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F };

  public boolean isAttached = false;

  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.DEBUG);
    try {
      OledSsd1306 oledSsd1306 = (OledSsd1306) Runtime.start("OledSsd1306", "OledSsd1306");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public OledSsd1306(String n) {
    super(n);
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");

    setDisplayType(SSD1306_128_64);
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }

  public void refreshControllers() {

    controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
    controllers.remove(this.getName());

    broadcastState();
  }

  @Override
  public void setDeviceBus(String deviceBus) {
    if (isAttached) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
      return;
    }
    this.deviceBus = deviceBus;
    broadcastState();
  }

  @Override
  public void setDeviceAddress(String deviceAddress) {
    if (isAttached) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
      return;
    }
    this.deviceAddress = deviceAddress;
    broadcastState();
  }

  /*
   * Initiate the buffer with data for the size of the OLED screen
   * 
   */
  public void setDisplayType(int displayType) {
    if (displayType == SSD1306_128_64) {
      SSD1306_LCDWIDTH = 128;
      SSD1306_LCDHEIGHT = 64;
      buffer = new int[SSD1306_LCDHEIGHT * SSD1306_LCDWIDTH / 8];
      buffer = SSD1306_128_64Data.clone();
    } else if (displayType == SSD1306_128_32) {
      SSD1306_LCDWIDTH = 128;
      SSD1306_LCDHEIGHT = 32;
      buffer = new int[SSD1306_LCDHEIGHT * SSD1306_LCDWIDTH / 8];
      buffer = SSD1306_128_32Data.clone();
    } else if (displayType == SSD1306_96_16) {
      SSD1306_LCDWIDTH = 96;
      SSD1306_LCDHEIGHT = 16;
      buffer = new int[SSD1306_LCDHEIGHT * SSD1306_LCDWIDTH / 8];
      buffer = SSD1306_96_16Data.clone();
    } else {
      log.error(String.format("DisplayType %s not implemented.", displayType));
    }
  }

  int getWidth() {
    return SSD1306_LCDWIDTH;
  }

  int getHeight() {
    return SSD1306_LCDHEIGHT;
  }

  public synchronized void setPixel(int x, int y, boolean on) {
    final int pos = x + (y / 8) * SSD1306_LCDWIDTH;
    if (on) {
      this.buffer[pos] |= (1 << (y & 0x07));
    } else {
      this.buffer[pos] &= ~(1 << (y & 0x07));
    }
  }

  void drawBitmap(int x, int y, int[] bitmap, int w, int h, boolean on) {

    int i, j, byteWidth = (w + 7) / 8;
    int aByte = 0;

    for (j = 0; j < h; j++) {
      for (i = 0; i < w; i++) {
        if ((i & 7) > 0) {
          aByte <<= 1;
        } else {
          aByte = bitmap[j * byteWidth + i / 8];
        }

        if ((aByte & 0x80) > 0) {
          setPixel(x + i, y + j, on);
        } else {
          setPixel(x + i, y + j, !on);
        }
      }
    }
  }

  public synchronized void drawString(String string, int x, int y, boolean on) {
    int posX = x;
    int posY = y;
    for (char c : string.toCharArray()) {
      if (c == '\n') {
        posY += 8;
        posX = x;
      }
      if (posX >= 0 && posX + 5 < this.getWidth() && posY >= 0 && posY + 7 < this.getHeight()) {
        drawChar(c, posX, posY, on);
      }
      posX += 6;
    }
  }

  public synchronized void drawStringCentered(String string, int y, boolean on) {
    final int strSizeX = string.length() * 5 + string.length() - 1;
    final int x = (this.getWidth() - strSizeX) / 2;
    drawString(string, x, y, on);
  }

  public synchronized void clearRect(int x, int y, int width, int height, boolean on) {
    for (int posX = x; posX < x + width; ++posX) {
      for (int posY = y; posY < y + height; ++posY) {
        setPixel(posX, posY, on);
      }
    }
  }

  public synchronized void drawChar(char c, int x, int y, boolean on) {
    if (c > 255) {
      c = '?';
    }

    for (int i = 0; i < 5; ++i) {
      int line = FONT[(c * 5) + i];

      for (int j = 0; j < 8; ++j) {
        if ((line & 0x01) > 0) {
          setPixel(x + i, y + j, on);
        }
        line >>= 1;
      }
    }
  }

  /*
   * draws the given image over the current image buffer. The image is
   * automatically converted to a binary image (if it not already is).
   * 
   * Note that the current buffer is not cleared before, so if you want the
   * image to completely overwrite the current display content you need to call
   * clear() before.
   *
   */
  public synchronized void drawImage(BufferedImage image, int x, int y) {
    BufferedImage tmpImage = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
    tmpImage.getGraphics().drawImage(image, x, y, null);

    int index = 0;
    int pixelval;
    final byte[] pixels = ((DataBufferByte) tmpImage.getRaster().getDataBuffer()).getData();
    for (int posY = 0; posY < SSD1306_LCDHEIGHT; posY++) {
      for (int posX = 0; posX < SSD1306_LCDWIDTH / 8; posX++) {
        for (int bit = 0; bit < 8; bit++) {
          pixelval = (byte) ((pixels[index / 8] >> (8 - bit)) & 0x01);
          setPixel(posX * 8 + bit, posY, pixelval > 0);
          index++;
        }
      }
    }
  }

  void begin(int vccstate) {
    this.vccstate = vccstate;

    // Init sequence
    ssd1306_command(SSD1306_DISPLAYOFF); // 0xAE
    ssd1306_command(SSD1306_SETDISPLAYCLOCKDIV); // 0xD5
    ssd1306_command(0x80); // the suggested ratio 0x80

    ssd1306_command(SSD1306_SETMULTIPLEX); // 0xA8
    ssd1306_command(SSD1306_LCDHEIGHT - 1);

    ssd1306_command(SSD1306_SETDISPLAYOFFSET); // 0xD3
    ssd1306_command(0x0); // no offset
    ssd1306_command(SSD1306_SETSTARTLINE | 0x0); // line #0
    ssd1306_command(SSD1306_CHARGEPUMP); // 0x8D
    if (vccstate == SSD1306_EXTERNALVCC) {
      ssd1306_command(0x10);
    } else {
      ssd1306_command(0x14);
    }
    ssd1306_command(SSD1306_MEMORYMODE); // 0x20
    ssd1306_command(0x00); // 0x0 act like ks0108
    ssd1306_command(SSD1306_SEGREMAP | 0x1);
    ssd1306_command(SSD1306_COMSCANDEC);

    if (oledType == SSD1306_128_32) {
      ssd1306_command(SSD1306_SETCOMPINS); // 0xDA
      ssd1306_command(0x02);
      ssd1306_command(SSD1306_SETCONTRAST); // 0x81
      ssd1306_command(0x8F);
    } else if (oledType == SSD1306_128_64) {
      ssd1306_command(SSD1306_SETCOMPINS); // 0xDA
      ssd1306_command(0x12);
      ssd1306_command(SSD1306_SETCONTRAST); // 0x81
      if (vccstate == SSD1306_EXTERNALVCC) {
        ssd1306_command(0x9F);
      } else {
        ssd1306_command(0xCF);
      }
    } else if (oledType == SSD1306_96_16) {
      ssd1306_command(SSD1306_SETCOMPINS); // 0xDA
      ssd1306_command(0x2); // ada x12
      ssd1306_command(SSD1306_SETCONTRAST); // 0x81
      if (vccstate == SSD1306_EXTERNALVCC) {
        ssd1306_command(0x10);
      } else {
        ssd1306_command(0xAF);
      }
    }

    ssd1306_command(SSD1306_SETPRECHARGE); // 0xd9
    if (vccstate == SSD1306_EXTERNALVCC) {
      ssd1306_command(0x22);
    } else {
      ssd1306_command(0xF1);
    }
    ssd1306_command(SSD1306_SETVCOMDETECT); // 0xDB
    ssd1306_command(0x40);
    ssd1306_command(SSD1306_DISPLAYALLON_RESUME); // 0xA4
    ssd1306_command(SSD1306_NORMALDISPLAY); // 0xA6

    ssd1306_command(SSD1306_DEACTIVATE_SCROLL);

    ssd1306_command(SSD1306_DISPLAYON);// --turn on oled panel
  }

  void invertDisplay(boolean i) {
    if (i) {
      ssd1306_command(SSD1306_INVERTDISPLAY);
    } else {
      ssd1306_command(SSD1306_NORMALDISPLAY);
    }
  }

  void ssd1306_command(int c) {

    // I2C
    byte control = 0x00; // Co = 0, D/C = 0
    byte buffer[] = { control, (byte) c };
    controller.i2cWrite((I2CControl) this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), buffer, buffer.length);
  }

  // startscrollright
  // Activate a right handed scroll for rows start through stop
  // Hint, the display is 16 rows tall. To scroll the whole display, run:
  // display.scrollright(0x00, 0x0F)
  void startscrollright(int start, int stop) {
    ssd1306_command(SSD1306_RIGHT_HORIZONTAL_SCROLL);
    ssd1306_command(0X00);
    ssd1306_command(start);
    ssd1306_command(0X00);
    ssd1306_command(stop);
    ssd1306_command(0X00);
    ssd1306_command(0XFF);
    ssd1306_command(SSD1306_ACTIVATE_SCROLL);
  }

  // startscrollleft
  // Activate a right handed scroll for rows start through stop
  // Hint, the display is 16 rows tall. To scroll the whole display, run:
  // display.scrollright(0x00, 0x0F)
  void startscrollleft(int start, int stop) {
    ssd1306_command(SSD1306_LEFT_HORIZONTAL_SCROLL);
    ssd1306_command(0X00);
    ssd1306_command(start);
    ssd1306_command(0X00);
    ssd1306_command(stop);
    ssd1306_command(0X00);
    ssd1306_command(0XFF);
    ssd1306_command(SSD1306_ACTIVATE_SCROLL);
  }

  // startscrolldiagright
  // Activate a diagonal scroll for rows start through stop
  // Hint, the display is 16 rows tall. To scroll the whole display, run:
  // display.scrollright(0x00, 0x0F)
  void startscrolldiagright(int start, int stop) {
    ssd1306_command(SSD1306_SET_VERTICAL_SCROLL_AREA);
    ssd1306_command(0X00);
    ssd1306_command(SSD1306_LCDHEIGHT);
    ssd1306_command(SSD1306_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL);
    ssd1306_command(0X00);
    ssd1306_command(start);
    ssd1306_command(0X00);
    ssd1306_command(stop);
    ssd1306_command(0X01);
    ssd1306_command(SSD1306_ACTIVATE_SCROLL);
  }

  // startscrolldiagleft
  // Activate a diagonal scroll for rows start through stop
  // Hint, the display is 16 rows tall. To scroll the whole display, run:
  // display.scrollright(0x00, 0x0F)
  void startscrolldiagleft(int start, int stop) {
    ssd1306_command(SSD1306_SET_VERTICAL_SCROLL_AREA);
    ssd1306_command(0X00);
    ssd1306_command(SSD1306_LCDHEIGHT);
    ssd1306_command(SSD1306_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL);
    ssd1306_command(0X00);
    ssd1306_command(start);
    ssd1306_command(0X00);
    ssd1306_command(stop);
    ssd1306_command(0X01);
    ssd1306_command(SSD1306_ACTIVATE_SCROLL);
  }

  void stopscroll() {
    ssd1306_command(SSD1306_DEACTIVATE_SCROLL);
  }

  // Dim the display
  // dim = true: display is dimmed
  // dim = false: display is normal
  void dim(boolean dim) {
    int contrast;

    if (dim) {
      contrast = 0; // Dimmed display
    } else {
      if (vccstate == SSD1306_EXTERNALVCC) {
        contrast = 0x9F;
      } else {
        contrast = 0xCF;
      }
    }
    // the range of contrast to too small to be really useful
    // it is useful to dim the display
    ssd1306_command(SSD1306_SETCONTRAST);
    ssd1306_command(contrast);
  }

  public void display(int[] image) {
    buffer = image.clone();
    display();
  }

  public void display() {
    ssd1306_command(SSD1306_COLUMNADDR);
    ssd1306_command(0); // Column start address (0 = reset)
    ssd1306_command(SSD1306_LCDWIDTH - 1); // Column end address (127 = reset)

    ssd1306_command(SSD1306_PAGEADDR);
    ssd1306_command(0); // Page start address (0 = reset)
    if (SSD1306_LCDHEIGHT == 64) {
      ssd1306_command(7); // Page end address
    }
    if (SSD1306_LCDHEIGHT == 32) {
      ssd1306_command(3); // Page end address
    }
    if (SSD1306_LCDHEIGHT == 16) {
      ssd1306_command(1); // Page end address
    }

    // I2C
    for (int i = 0; i < (SSD1306_LCDWIDTH * SSD1306_LCDHEIGHT / 8) - 1; i++) {
      // send a bunch of data in one xmission
      // Wire.beginTransmission(_i2caddr);
      byte writeBuffer[] = new byte[17];
      int writeBufferIx = 0;
      writeBuffer[writeBufferIx] = 0x40;
      for (int x = 0; x < 16; x++) {
        writeBufferIx++;
        writeBuffer[writeBufferIx] = (byte) buffer[i];
        // WIRE_WRITE(buffer[i]);
        i++;
      }
      i--;
      // Wire.endTransmission();
      controller.i2cWrite((I2CControl) this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writeBuffer, writeBuffer.length);
    }

  }

  // clear everything
  void clearDisplay() {
    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = 0;
    }
  }

  // fill everything
  void fillDisplay() {
    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = 0xff;
    }
  }

  void drawFastHLine(int x, int y, int w, int color) {
    boolean bSwap = false;
    int tmpx;
    switch (rotation) {
      case 0:
        // 0 degree rotation, do nothing
        break;
      case 1:
        // 90 degree rotation, swap x & y for rotation, then invert x
        bSwap = true;
        tmpx = x;
        x = y;
        y = tmpx;
        x = SSD1306_LCDWIDTH - x - 1;
        break;
      case 2:
        // 180 degree rotation, invert x and y - then shift y around for height.
        x = SSD1306_LCDWIDTH - x - 1;
        y = SSD1306_LCDHEIGHT - y - 1;
        x -= (w - 1);
        break;
      case 3:
        // 270 degree rotation, swap x & y for rotation, then invert y and
        // adjust
        // y for w (not to become h)
        bSwap = true;
        tmpx = x;
        x = y;
        y = tmpx;
        y = SSD1306_LCDHEIGHT - y - 1;
        y -= (w - 1);
        break;
    }

    if (bSwap) {
      drawFastVLineInternal(x, y, w, color);
    } else {
      drawFastHLineInternal(x, y, w, color);
    }
  }

  void drawFastHLineInternal(int x, int y, int w, int color) {
    // Do bounds/limit checks
    if (y < 0 || y >= SSD1306_LCDHEIGHT) {
      return;
    }

    // make sure we don't try to draw below 0
    if (x < 0) {
      w += x;
      x = 0;
    }

    // make sure we don't go off the edge of the display
    if ((x + w) > SSD1306_LCDWIDTH) {
      w = (SSD1306_LCDWIDTH - x);
    }

    // if our width is now negative, punt
    if (w <= 0) {
      return;
    }

    // set up the pointer for movement through the buffer
    // int *pBuf = buffer;
    int pBuf = 0;
    // adjust the buffer pointer for the current row
    pBuf += ((y / 8) * SSD1306_LCDWIDTH);
    // and offset x columns in
    pBuf += x;

    int mask = 1 << (y & 7);

    switch (color) {
      case WHITE:
        while (w > 0) {
          buffer[pBuf] = buffer[pBuf] | mask;
          pBuf++;
          w--;
        }
        ;
        break;
      case BLACK:
        mask = ~mask;
        while (w > 0) {
          buffer[pBuf] = buffer[pBuf] & mask;
          pBuf++;
          w--;
        }
        ;
        break;
      case INVERSE:
        while (w > 0) {
          buffer[pBuf] = buffer[pBuf] ^ mask;
          pBuf++;
          w--;
        }
        ;
        break;
    }
  }

  void drawFastVLine(int x, int y, int h, int color) {
    boolean bSwap = false;
    int tmpx;
    switch (rotation) {
      case 0:
        break;
      case 1:
        // 90 degree rotation, swap x & y for rotation, then invert x and adjust
        // x
        // for h (now to become w)
        bSwap = true;
        tmpx = x;
        x = y;
        y = tmpx;
        x = SSD1306_LCDWIDTH - x - 1;
        x -= (h - 1);
        break;
      case 2:
        // 180 degree rotation, invert x and y - then shift y around for height.
        x = SSD1306_LCDWIDTH - x - 1;
        y = SSD1306_LCDHEIGHT - y - 1;
        y -= (h - 1);
        break;
      case 3:
        // 270 degree rotation, swap x & y for rotation, then invert y
        bSwap = true;
        tmpx = x;
        x = y;
        y = tmpx;
        y = SSD1306_LCDHEIGHT - y - 1;
        break;
    }

    if (bSwap) {
      drawFastHLineInternal(x, y, h, color);
    } else {
      drawFastVLineInternal(x, y, h, color);
    }
  }

  void drawFastVLineInternal(int x, int __y, int __h, int color) {

    // do nothing if we're off the left or right side of the screen
    if (x < 0 || x >= SSD1306_LCDWIDTH) {
      return;
    }

    // make sure we don't try to draw below 0
    if (__y < 0) {
      // __y is negative, this will subtract enough from __h to account for __y
      // being 0
      __h += __y;
      __y = 0;

    }

    // make sure we don't go past the height of the display
    if ((__y + __h) > SSD1306_LCDHEIGHT) {
      __h = (SSD1306_LCDHEIGHT - __y);
    }

    // if our height is now negative, punt
    if (__h <= 0) {
      return;
    }

    // this display doesn't need ints for coordinates, use local byte registers
    // for faster juggling
    int y = __y;
    int h = __h;
    int mod = y & 7;

    // set up the pointer for fast movement through the buffer
    int pBuf = 0;
    // adjust the buffer pointer for the current row
    pBuf += ((y / 8) * SSD1306_LCDWIDTH);
    // and offset x columns in
    pBuf += x;

    // do the first partial byte, if necessary - this requires some masking

    if (mod > 0) {
      // mask off the high n bits we want to set
      mod = 8 - mod;

      // note - lookup table results in a nearly 10% performance improvement in
      // fill* functions
      // register int mask = ~(0xFF >> (mod));

      int mask = premask[mod];

      // adjust the mask if we're not going to reach the end of this byte
      if (h < mod) {
        mask &= (0XFF >> (mod - h));
      }

      switch (color) {
        case WHITE:
          buffer[pBuf] = buffer[pBuf] | mask;
          break;
        case BLACK:
          buffer[pBuf] = buffer[pBuf] & ~mask;
          break;
        case INVERSE:
          buffer[pBuf] = buffer[pBuf] ^ mask;
          break;
      }

      // fast exit if we're done here!
      if (h < mod) {
        return;
      }

      h -= mod;

      pBuf = pBuf + SSD1306_LCDWIDTH;
    }

    // write solid bytes while we can - effectively doing 8 rows at a time
    if (h >= 8) {
      if (color == INVERSE) { // separate copy of the code so we don't impact
                              // performance of the black/white write version
                              // with an extra comparison per loop
        do {
          buffer[pBuf] = ~(buffer[pBuf]);

          // adjust the buffer forward 8 rows worth of data
          pBuf += SSD1306_LCDWIDTH;

          // adjust h & y (there's got to be a faster way for me to do this, but
          // this should still help a fair bit for now)
          h -= 8;
        } while (h >= 8);
      } else {
        // store a local value to work with
        int val = (color == WHITE) ? 255 : 0;

        do {
          // write our value in
          buffer[pBuf] = val;

          // adjust the buffer forward 8 rows worth of data
          pBuf += SSD1306_LCDWIDTH;

          // adjust h & y (there's got to be a faster way for me to do this, but
          // this should still help a fair bit for now)
          h -= 8;
        } while (h >= 8);
      }
    }

    // now do the final partial byte, if necessary
    if (h > 0) {
      mod = h & 7;
      // this time we want to mask the low bits of the byte, vs the high bits we
      // did above
      // register int mask = (1 << mod) - 1;
      // note - lookup table results in a nearly 10% performance improvement in
      // fill* functions
      int mask = postmask[mod];
      switch (color) {
        case WHITE:
          buffer[pBuf] = buffer[pBuf] | mask;
          break;
        case BLACK:
          buffer[pBuf] = buffer[pBuf] & ~mask;
          break;
        case INVERSE:
          buffer[pBuf] = buffer[pBuf] ^ mask;
          break;
      }
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

    ServiceType meta = new ServiceType(OledSsd1306.class.getCanonicalName());
    meta.addDescription("OLED driver using SSD1306 driver and the i2c protocol");
    meta.addCategory("i2c", "control");
    meta.setAvailable(true);
    meta.setSponsor("Mats");
    return meta;
  }

  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  // This section contains all the new attach logic
  @Override
  public void attach(String service) throws Exception {
    attach((Attachable) Runtime.getService(service));
  }

  @Override
  public void attach(Attachable service) throws Exception {

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      attachI2CController((I2CController) service);
      return;
    }
  }

  public void attach(String controllerName, String deviceBus, String deviceAddress) {
    attach((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
  }

  public void attach(I2CController controller, String deviceBus, String deviceAddress) {

    if (isAttached && this.controller != controller) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
    }

    controllerName = controller.getName();
    log.info(String.format("%s attach %s", getName(), controllerName));

    this.deviceBus = deviceBus;
    this.deviceAddress = deviceAddress;

    attachI2CController(controller);
    isAttached = true;
    broadcastState();
  }

  public void attachI2CController(I2CController controller) {

    if (isAttached(controller))
      return;

    if (this.controllerName != controller.getName()) {
      log.error(String.format("Trying to attached to %s, but already attached to (%s)", controller.getName(), this.controllerName));
      return;
    }

    this.controller = controller;
    isAttached = true;
    controller.attachI2CControl(this);
    log.info(String.format("Attached %s device on bus: %s address %s", controllerName, deviceBus, deviceAddress));
    broadcastState();
  }

  // This section contains all the new detach logic
  // TODO: This default code could be in Attachable
  @Override
  public void detach(String service) {
    detach((Attachable) Runtime.getService(service));
  }

  @Override
  public void detach(Attachable service) {
    
    if (service!=null)
    if (I2CController.class.isAssignableFrom(service.getClass())) {
      detachI2CController((I2CController) service);
      return;
    }
  }

  @Override
  public void detachI2CController(I2CController controller) {

    if (!isAttached(controller))
      return;

    controller.detachI2CControl(this);
    isAttached = false;
    broadcastState();
  }
  
  
  @Override
  public void stopService() {

    if (isAttached(controller))
    {
    controller.detachI2CControl(this);
    }
  }

  // This section contains all the methods used to query / show all attached
  // methods
  /**
   * Returns all the currently attached services
   */
  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null && isAttached) {
      ret.add(controller.getName());
    }
    return ret;
  }

  @Override
  public String getDeviceBus() {
    return this.deviceBus;
  }

  @Override
  public String getDeviceAddress() {
    return this.deviceAddress;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    if (controller != null && controller.getName().equals(instance.getName())) {
      return isAttached;
    }
    ;
    return false;
  }
}