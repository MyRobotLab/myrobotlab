package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.BoardType;

// FIXME add SensorController
public interface Microcontroller extends PinArrayControl {

	public String getBoard();

	public BoardInfo getBoardInfo();
	
	/**
	 * a static request to return all possible "BoardTypes" which this service supports.
	 * Arduino would return many, as there are many board types of arduino.
	 * 
	 * Not sure if Beagle would "need" to return different types, unless it has different
	 * pin definitions for different variations.
	 * 
	 * Parallax/Prop I think has several boards
	 * @return list of board types
	 */
	public List<BoardType> getBoardTypes();
}
