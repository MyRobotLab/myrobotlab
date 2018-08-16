package org.myrobotlab.math;

public interface MapperInterface {

  double calcOutput(double in);

  int calcOutputInt(int in);

  int calcOutputInt(double in);

  double getMinOutput();

  double getMaxOutput();

  void setMinMaxOutput(double minOutput, double maxOutput);

  double getMinInput();

  double getMaxInput();

  void setMinMaxInput(double minInput, double maxInput);

  void merge(MapperInterface mapperInterface);

  void map(double minX, double maxX, double minY, double maxY);

}