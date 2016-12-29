package org.myrobotlab.framework;

import java.io.*;
import java.util.*;

public class CreateStarter {

  private Platform p = Platform.getLocalInstance();
  private String myRuntimeName = "runtime";
  private boolean myLogToConsole = false;
  private String myLogLevel = "INFO";
  private String myServiceName;
  private String myServiceType;

  public CreateStarter() {
  }
  
  public CreateStarter(String serviceType) {
    myServiceName = "my" + serviceType;
    myServiceType = serviceType;
  }
  
  public CreateStarter(String serviceName, String serviceType) {
    myServiceName = serviceName;
    myServiceType = serviceType;
  }
  
  public void setRuntimeName(String name) {
    myRuntimeName = name;
  }
  
  public String getRuntimeName() {
    return myRuntimeName;
  }
  
  public void setLogToConsole(boolean val) {
    myLogToConsole = val;
  }
  
  public boolean getLogToConsole() {
    return myLogToConsole;
  }

  public void setLogLevel(String level) {
    String myLevel = level.toUpperCase();
    if (myLevel.equals("DEBUG") || myLevel.equals("INFO")  || myLevel.equals("WARNING") || myLevel.equals("ERROR") || myLevel.equals("FATAL")) {
      myLogLevel = level;
    } else {
      System.err.println("Wrong error level '" + level +  "'.");
    }
  }
  
  public String getLogLevel() {
    return myLogLevel;
  }

  public void setServiceName(String name) {
    myServiceName = name;
  }
  
  public String getServiceName() {
    return myServiceName;
  }

  public void setServiceType(String type) {
    myServiceType = type;
  }
  
  public String getServiceType() {
    return myServiceType;
  }
  
  public void createServiceStarter() {
    File myFile;
    if (p.isWindows())
      myFile = new File(getServiceType() + ".bat");
    else
      myFile = new File(getServiceType() + ".sh");
     
    try {
      FileWriter writer = new FileWriter(myFile);
      if (p.isWindows())
        writer.write("@echo off");
      else
        writer.write("#!/bin/sh");
      writer.write(p.getNewLineSeparator());
      writer.write(getJavaCommand() + " -classpath " + getClassPath() + " org.myrobotlab.service.Runtime");
      writer.write(" -runtimeName " + getRuntimeName());
      if (getLogToConsole())
        writer.write(" -logToConsole");
      writer.write(" -logLevel " + getLogLevel());
      writer.write(" -service " + getServiceName() + " " + getServiceType());
      writer.write(p.getNewLineSeparator());
      writer.flush();
      writer.close();
    } catch (IOException e) {
      System.err.println(e);
    }
    myFile.setExecutable(true);
    myFile.setReadable(true);
    myFile.setWritable(true);
  } 
  
  public void createEmptyRuntimeStarter() {
    File myFile;
    if (p.isWindows())
      myFile = new File("Runtime.bat");
    else
      myFile = new File("Runtime.sh");
     
    try {
      FileWriter writer = new FileWriter(myFile);
      if (p.isWindows())
        writer.write("@echo off");
      else
        writer.write("#!/bin/sh");
      writer.write(p.getNewLineSeparator());
      writer.write(getJavaCommand() + " -classpath " + getClassPath() + " org.myrobotlab.service.Runtime ");
      if (p.isWindows())
        writer.write("%1 %2 %3 %4 %5 %6 %7 %8 %9");
      else
        writer.write("$*");
      writer.write(p.getNewLineSeparator());
      writer.flush();
      writer.close();
    } catch (IOException e) {
      System.err.println(e);
    }
    myFile.setExecutable(true);
    myFile.setReadable(true);
    myFile.setWritable(true);
  }
  
  private File[] getJarFiles() {
    String dir = System.getProperties().getProperty("user.dir") + p.getDirectorySeparator() + "libraries" + p.getDirectorySeparator() + "jar";
    File[] retFiles = new File[0];

    File myDir = new File(dir);
    File[] myFiles = myDir.listFiles();
    
    for (File myFile : myFiles) {
      if (myFile.isFile() && myFile.canRead() && myFile.getName().substring(myFile.getName().length()-4  ,myFile.getName().length()).equalsIgnoreCase(".jar")) {
        retFiles = Arrays.copyOf(retFiles, retFiles.length + 1);
        retFiles[retFiles.length-1] = myFile;
      }
    }    
    return retFiles;
  }
  
  // possible this is later no longer needed, when a own ClassLoader is created, which loads the jars automatical after start
  private String getClassPath() {
    String retString = "myrobotlab.jar";
    for (int i=0; i<=getJarFiles().length-1; i++) {
      retString += p.getClassPathSeparator() + getJarFiles()[i].getAbsoluteFile();
    }
    return retString;
  }
  
  private String getJavaCommand() {
    return System.getProperties().getProperty("java.home") + p.getDirectorySeparator() + "bin" + p.getDirectorySeparator() + "java";
  }
  
  public static void main(String[] args) {
    
    if (args.length == 1) {
      CreateStarter cs = new CreateStarter(args[0]);
      cs.createServiceStarter();
      cs.createEmptyRuntimeStarter();
    } else if (args.length == 2) {
      CreateStarter cs = new CreateStarter(args[0], args[1]);
      cs.createServiceStarter();
      cs.createEmptyRuntimeStarter();
    } else {
      System.out.println("Start with");
      System.out.println("java org.myrobotlab.framework.CreateStarter [[Service Name] Service Type]");
    }
    
  }
  
}
