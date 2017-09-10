package org.myrobotlab.arduino;

// FIXME FIXME FIXME !!! - this should not be in the Arduino package
public class BoardType {

  /**
   * display name of the board - this will be what a user sees in the dropdown
   */
  String name;
  
  /**
   * unique string key of the board
   * e.g. for Arduino mega, nano, uno, ...
   */
  String board;
  
  /**
   * unique identifier of the board
   */
  Integer id;
  
  
  public String toString(){
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public void setBoard(String board) {
    this.board = board;
  }
  
  public void setId(int id) {
    this.id = id;
  }
  
  public String getName() {
    return name;
  }


  public String getBoard() {
    return board;
  }
  
  public int getId() {
    return id;
  }
  
}
