package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.slf4j.Logger;

/**
 * ported from https://github.com/aanon4/FlySkyIBus
 * 
 * @author aanon4
 *
 */
public class IBus extends Service<ServiceConfig> implements SerialDataListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(IBus.class);

  // enum State
  // {
  static final int GET_LENGTH = 0;
  static final int GET_DATA = 1;
  static final int GET_CHKSUML = 2;
  static final int GET_CHKSUMH = 3;
  static final int DISCARD = 4;
  // };

  static final int PROTOCOL_LENGTH = 0x20;
  static final int PROTOCOL_OVERHEAD = 3; // <len><cmd><data....><chkl><chkh>
  static final int PROTOCOL_TIMEGAP = 3; // Packets are received very ~7ms so
  // use ~half that for the gap
  static final int PROTOCOL_CHANNELS = 10;
  static final int PROTOCOL_COMMAND40 = 0x40; // Command is always 0x40

  int state;
  // Stream* stream;
  long last;
  int[] buffer = new int[PROTOCOL_LENGTH];
  int ptr;
  int len;
  int[] channel = new int[PROTOCOL_CHANNELS];
  int chksum;
  int lchksum;

  SerialDevice serial;

  public IBus(String n, String id) {
    super(n, id);
  }

  public void attach(SerialDevice serial) throws Exception {
    this.serial = serial;
    serial.attach(serial);
  }

  static long millis() {
    return System.currentTimeMillis();
  }

  void begin() {
    // this.stream = &stream;
    this.state = DISCARD;
    this.last = millis();
    this.ptr = 0;
    this.len = 0;
    this.chksum = 0;
    this.lchksum = 0;
  }

  @Override
  public void onBytes(byte[] bytes) {
    for (int j = 0; j < bytes.length; j++) {
      Integer b = bytes[j] & 0xFF;
      long now = millis();
      if (now - last >= PROTOCOL_TIMEGAP) {
        state = GET_LENGTH;
      }
      last = now;

      int v = b;
      switch (state) {
        case GET_LENGTH:
          if (v <= PROTOCOL_LENGTH) {
            ptr = 0;
            len = v - PROTOCOL_OVERHEAD;
            chksum = 0xFFFF - v;
            state = GET_DATA;
          } else {
            state = DISCARD;
          }
          break;

        case GET_DATA:
          buffer[ptr++] = v;
          chksum -= v;
          if (ptr == len) {
            state = GET_CHKSUML;
          }
          break;

        case GET_CHKSUML:
          lchksum = v;
          state = GET_CHKSUMH;
          break;

        case GET_CHKSUMH:
          // Validate checksum
          if (chksum == (v << 8) + lchksum) {
            // Execute command - we only know command 0x40
            switch (buffer[0]) {
              case PROTOCOL_COMMAND40:
                // Valid - extract channel data
                for (int i = 1; i < PROTOCOL_CHANNELS * 2 + 1; i += 2) {
                  channel[i / 2] = buffer[i] | (buffer[i + 1] << 8);
                }
                invoke("publishChannel", channel);
                break;

              default:
                break;
            }
          }
          state = DISCARD;
          break;

        case DISCARD:
        default:
          break;
      }
    }
  }

  public int[] publishChanel(int[] channel) {
    return channel;
  }

  public int readChannel(int channelNr) {
    if (channelNr < PROTOCOL_CHANNELS) {
      return channel[channelNr];
    } else {
      return 0;
    }
  }

  @Override
  public void onConnect(String portName) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onDisconnect(String portName) {
    // TODO Auto-generated method stub

  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("ibus", "IBus");
      Runtime.start("servo", "Servo");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}

// yum yum ...