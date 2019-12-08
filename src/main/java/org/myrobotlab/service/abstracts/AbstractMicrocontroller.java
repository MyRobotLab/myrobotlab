package org.myrobotlab.service.abstracts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;

public abstract class AbstractMicrocontroller extends Service implements Microcontroller {

  private static final long serialVersionUID = 1L;

  /**
   * board type - UNO Mega etc..
   * 
   * if the user 'connects' first then the info could come from the board .. but
   * if the user wants to upload first a npe will be thrown so we default it
   * here to Uno
   */
  protected String board;
  
  /**
   * Some boards have the ability to send identification which allow identification of the board type
   * during runtime.  Arduino has the ability to send the BOARD value which is an identifier put into the code
   * as a define from a compiler directive.  Overall we want to default to "most common" value, allow the board to try to 
   * set the value, or allow the user to set the value.  If the "user" sets the value, the board should accept that value
   * and not be changed internally. So after a command to set the board exists, the board will be "locked" meaning
   * the data will not be reset.
   */
  protected String userBoardType = null;

  /**
   * other services subscribed to pins
   */
  transient protected Map<String, PinArrayListener> pinArrayListeners = new ConcurrentHashMap<String, PinArrayListener>();

  /**
   * address index of pins - this is the "true" representation of pins as
   * whatever its documented to people e.g. "A5" or "D7", it comes down to a
   * unique address
   */
  protected Map<Integer, PinDefinition> pinIndex = new TreeMap<>();

  /**
   * map of pin listeners
   */
  transient protected Map<Integer, Set<PinListener>> pinListeners = new ConcurrentHashMap<>();

  /**
   * name index of pinList
   */
  protected Map<String, PinDefinition> pinMap = new TreeMap<>();

  /**
   * possible types of boards
   */
  ArrayList<BoardType> boardTypes;

  /**
   * @param degree
   *          degrees
   * @return degreeToMicroseconds - convert a value to send to servo from degree
   *         (0-180) to microseconds (544-2400)
   * 
   */
  static public Integer degreeToMicroseconds(double degree) {
    return (int) Math.round((degree * (2400 - 544) / 180) + 544);
  }

  public AbstractMicrocontroller(String reservedKey, String id) {
    super(reservedKey, id);
  }

  /**
   * attach a pin listener which listens for an array of all active pins
   */
  @Override
  public void attach(PinArrayListener listener) {
    pinArrayListeners.put(listener.getName(), listener);
    // TODO: re-implement this.. it seemed unstable when i was testing before.
    // attaching a pin listener should enable the pins (if they're not already enabled.)
    // if the pin array listener is listening for a specific set of pins, we should enable those.
    // if (listener.getActivePins()!= null && listener.getActivePins().length >0) {
    //          for (String pin : listener.getActivePins()) {
    //            // TODO: what rate?
    //            // TODO: maybe expose rate as a parameter for the listener to supply
    //            log.info("Enable pin {}", pin);
    //            int rate = 0;
    //            this.enablePin(pin, rate);
    //    
    // }
    // }

  }

  /**
   * attach a pin listener who listens to a specific pin
   */
  @Override
  public void attach(PinListener listener, int address) {
    String name = listener.getName();

    if (listener.isLocal()) {
      Set<PinListener> list = null;
      if (pinListeners.containsKey(address)) {
        list = pinListeners.get(address);
      } else {
        list = new HashSet<PinListener>();
      }
      list.add(listener);
      pinListeners.put(address, list);

    } else {
      addListener("publishPin", name, "onPin");
    }
  }

  @Override
  public void attach(PinListener listener, String pin) {
    PinDefinition pinDef = getPin(pin);
    attach(listener, pinDef.getAddress());
  }

  @Override
  public void disablePin(String pin) {
    disablePin(getPin(pin).getAddress());
  }

  @Override
  abstract public void disablePin(int address);

  @Override
  public void disablePins() {
    for (PinDefinition pinDef : pinIndex.values()) {
      disablePin(pinDef.getAddress());
    }
  }

  @Override
  public void enablePin(String pin) {
    enablePin(getPin(pin).getAddress());
  }

  @Override
  abstract public void enablePin(int address);

  @Override
  public void enablePin(String pin, int rate) {
    enablePin(getPin(pin).getAddress(), rate);
  }

  @Override
  abstract public void enablePin(int address, int rate);

  @Override
  public PinDefinition getPin(String pinName) {
    if (pinMap.containsKey(pinName)) {
      return pinMap.get(pinName);
    }
    // log.error("pinMap does not contain pin {}", pinName);
    return null;
  }

  @Override
  public PinDefinition getPin(int address) {
    if (pinIndex.containsKey(address)) {
      return pinIndex.get(address);
    }
    log.error("pinIndex does not contain address {}", address);
    return null;
  }

  @Override
  abstract public List<PinDefinition> getPinList();

  @Override
  public void pinMode(String pin, String mode) {
    pinMode(getPin(pin).getAddress(), mode);
  }

  @Override
  abstract public void pinMode(int address, String mode);

  @Override
  public PinData publishPin(PinData pinData) {
    // cache the value
    pinMap.get(pinData.pin).setValue(pinData.value);
    return pinData;
  }

  @Override
  public PinData[] publishPinArray(PinData[] pinData) {
    return pinData;
  }

  /**
   * method to communicate changes in pinmode or state changes
   * 
   */
  @Override
  public PinDefinition publishPinDefinition(PinDefinition pinDef) {
    return pinDef;
  }

  @Override
  public int read(int address) {
    // FIXME - this would be "last" read
    return pinIndex.get(address).getValue();
  }

  @Override
  public int read(String pin) {
    PinDefinition pinDef = getPin(pin);
    return read(pinDef.getAddress());
  }

  @Override
  abstract public void reset();

  @Override
  public void write(String pin, int value) {
    PinDefinition pinDef = getPin(pin);
    write(pinDef.getAddress(), value);
  }

  @Override
  abstract public void write(int address, int value);

  /**
   * Identifier of "board type" from the possible set in boardTypes
   */
  @Override
  public String getBoard() {
    if (userBoardType != null) {
      // user has set board type - return it
      return userBoardType;
    }
    return board;
  }

  @Override
  abstract public BoardInfo getBoardInfo();

  @Override
  abstract public List<BoardType> getBoardTypes();

  @Override
  public String setBoard(String board) {
    log.debug("setting board to type {}", board);
    // user or program has manuall set the board
    // from this time forward - do not attempt to "auto-set"
    userBoardType = board; 
    
    log.warn("setting board to type {}", board);
    this.board = board;
    // we don't invoke, because
    // it might get into a race condition
    // in some gui
    getPinList();
    // invoke("getPinList");
    broadcastState();
    return board;
  }
  
}
