package org.myrobotlab.arduino;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class ArduinoMsgGenerator {

  public transient final static Logger log = LoggerFactory.getLogger(ArduinoMsgGenerator.class);

	static final Integer MRLCOMM_VERSION = 57;

  public void generateDefinitions() throws IOException {
    generateDefinitions(new File("src/resource/Arduino/generate/arduinoMsgs.schema"));
  }

  static final HashSet<String> keywords = new HashSet<String>();

  static final List<String> deviceList = new ArrayList<String>();

  /**
   * supresses building of MrlComm::{Name} method if method already exists in
   * Arduino
   * 
   */

  ArduinoMsgGenerator() {
    // add your keywords
    keywords.add("pinMode");
    keywords.add("digitalWrite");
    keywords.add("analogWrite");

    // ------ device type mapping constants

    // add your mrl devices
    deviceList.add("unknown");
    deviceList.add("Arduino");
    deviceList.add("UltrasonicSensor");
    deviceList.add("Stepper");
    deviceList.add("Motor");
    deviceList.add("Servo");
    deviceList.add("Serial");
    deviceList.add("I2c");
    deviceList.add("NeoPixel");
  }

  static public final String toString(String filename) throws IOException {

    FileInputStream is = new FileInputStream(filename);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {

      int nRead;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        baos.write(data, 0, nRead);
      }

      baos.flush();
      baos.close();
      is.close();
      return new String(baos.toByteArray());
    } catch (Exception e) {
      Logging.logError(e);
    }

    return null;
  }

  // FIXME - before it comes a mess - send and recv templates !!!

  public void generateDefinitions(File idl) throws IOException {

    // load templates
    String arduinoMsgCodeTemplateH = toString("src/resource/Arduino/generate/ArduinoMsgCodec.template.h");
    String idlToHpp = toString("src/resource/Arduino/generate/Msg.template.h");
    String idlToCpp = toString("src/resource/Arduino/generate/Msg.template.cpp");
    String idlToJava = toString("src/resource/Arduino/generate/Msg.template.java");
    String virtualJava = toString("src/resource/Arduino/generate/Msg.template.java");

    // String idlToJava = toString("blah");

    // search and replace
    Map<String, String> fileSnr = new TreeMap<String, String>();

    StringBuilder deviceTypeToString = new StringBuilder();
    StringBuilder cppDeviceTypes = new StringBuilder();
    StringBuilder javaDeviceTypes = new StringBuilder();
    for (int i = 0; i < deviceList.size(); ++i) {
      String deviceName = deviceList.get(i);
      deviceTypeToString.append("\t\tcase " + i + " :  {\n\t\t\treturn \"" + deviceName + "\";\n\n\t\t}\n");
      cppDeviceTypes.append("#define DEVICE_TYPE_" + deviceName.toUpperCase() + "\t\t" + i + "\n");
      javaDeviceTypes.append("\tpublic static final int DEVICE_TYPE_" + deviceName.toUpperCase() + "\t = \t\t" + i + ";\n");
    }

    // accumulators
    StringBuilder defines = new StringBuilder();

    StringBuilder methodToString = new StringBuilder();
    StringBuilder hMethods = new StringBuilder();
    StringBuilder cppMethods = new StringBuilder();
    StringBuilder cppHandleCases = new StringBuilder();
    StringBuilder cppGeneratedCallBacks = new StringBuilder();

    StringBuilder javaGeneratedCallBacks = new StringBuilder();
    StringBuilder javaDefines = new StringBuilder();
    StringBuilder javaMethods = new StringBuilder();
    StringBuilder javaHandleCases = new StringBuilder();

    StringBuilder vJavaGeneratedCallBacks = new StringBuilder();
    // StringBuilder vJavaDefines = new StringBuilder();
    StringBuilder vJavaMethods = new StringBuilder();
    StringBuilder vJavaHandleCases = new StringBuilder();

    // process schema
    BufferedReader br = new BufferedReader(new FileReader(idl));
    int methodIndex = 1;
    String line;
    while ((line = br.readLine()) != null) {
      line = line.trim();
      log.info("line {}", line);
      if (line.length() == 0 || line.charAt(0) == '#') {
        continue;
      }

      String[] parts = line.split("/");

      String begin = parts[0];

      char dir = begin.charAt(0);
      if (dir != '<' && dir != '>') {
        log.error("{} invalid expecting direction < or >", dir);
        continue;
      }
      int offset = 1;
      if (begin.charAt(0) == ' ') {
        offset++;
      }

      String name = begin.substring(offset).trim();

      log.info("method name {}", name);

      Map<String, String> methodData = perMsgMethod(methodIndex, line, dir, name, Arrays.copyOfRange(parts, 1, parts.length));

      methodToString.append("\t\tcase " + CodecUtils.toUnderScore(name) + ":{\n\t\t\treturn \"" + name + "\";\n\t\t}\n");

      // mux out
      defines.append(methodData.get("define"));

      hMethods.append(methodData.get("hMethod"));
      cppHandleCases.append(methodData.get("cppHandleCase"));
      cppMethods.append(methodData.get("cppMethod"));
      cppGeneratedCallBacks.append(methodData.get("cppGeneratedCallBacks"));

      javaDefines.append(methodData.get("javaDefine"));
      javaMethods.append(methodData.get("javaMethod"));
      javaHandleCases.append(methodData.get("javaHandleCase"));
      javaGeneratedCallBacks.append(methodData.get("javaGeneratedCallBack"));

      // vJavaDefines.append(methodData.get("vJavaDefine"));
      vJavaMethods.append(methodData.get("vJavaMethod"));
      vJavaHandleCases.append(methodData.get("vJavaHandleCase"));
      vJavaGeneratedCallBacks.append(methodData.get("vJavaGeneratedCallBack"));

      ++methodIndex;
    }

    br.close();

    fileSnr.put("%enableAck%", "enableAck(b);");
    
    // file templates
    fileSnr.put("%MRLCOMM_VERSION%", MRLCOMM_VERSION.toString());
    fileSnr.put("%defines%", defines.toString());
    fileSnr.put("%hMethods%", hMethods.toString());
    fileSnr.put("%cppMethods%", cppMethods.toString());
    fileSnr.put("%methodToString%", methodToString.toString());
    fileSnr.put("%cppHandleCases%", cppHandleCases.toString());

    fileSnr.put("%javaDefines%", javaDefines.toString());
    fileSnr.put("%javaHandleCases%", javaHandleCases.toString());
    fileSnr.put("%javaMethods%", javaMethods.toString());
    fileSnr.put("%generatedCallBacks%", cppGeneratedCallBacks.toString());
    fileSnr.put("%javaGeneratedCallBacks%", javaGeneratedCallBacks.toString());

    fileSnr.put("%deviceTypeToString%", deviceTypeToString.toString());
    fileSnr.put("%cppDeviceTypes%", cppDeviceTypes.toString());
    fileSnr.put("%javaDeviceTypes%", javaDeviceTypes.toString());

    // FIXME - will move to MrlComm.h
    String mrlComm_h = toString("src/resource/Arduino/MRLComm/MrlComm.h");
    String top = mrlComm_h.substring(0, mrlComm_h.indexOf("<generatedCallBacks>") + "<generatedCallBacks>".length());
    String bottom = mrlComm_h.substring(mrlComm_h.indexOf("</generatedCallBacks>"));
    FileOutputStream mrlComm_updated_h = new FileOutputStream("src/resource/Arduino/MRLComm/MrlComm.h");
    mrlComm_updated_h.write((top + "\n" + cppGeneratedCallBacks.toString() + "    // " + bottom).getBytes());
    mrlComm_updated_h.close();

    idlToJava = idlToJava.replace("%arduino%", "arduino");
    idlToJava = idlToJava.replace("%javaClass%", "Msg");
    idlToJava = idlToJava.replace("%javaArduinoClass%", "Arduino");

    virtualJava = virtualJava.replace("%arduino%", "virtual");
    virtualJava = virtualJava.replace("%javaClass%", "VirtualMsg");
    virtualJava = virtualJava.replace("%javaArduinoClass%", "MrlComm");

    fileSnr.put("%ackEnabled%", "true");
    // process substitutions
    for (String searchKey : fileSnr.keySet()) {
      idlToHpp = idlToHpp.replace(searchKey, fileSnr.get(searchKey));
      idlToCpp = idlToCpp.replace(searchKey, fileSnr.get(searchKey));
      idlToJava = idlToJava.replace(searchKey, fileSnr.get(searchKey));
      arduinoMsgCodeTemplateH = arduinoMsgCodeTemplateH.replace(searchKey, fileSnr.get(searchKey));
    }

    // replace java tags with virtual java tags so we can use the same
    // template
    fileSnr.put("%javaDefines%", javaDefines.toString());
    fileSnr.put("%javaHandleCases%", vJavaHandleCases.toString());
    fileSnr.put("%javaMethods%", vJavaMethods.toString());
    fileSnr.put("%javaGeneratedCallBacks%", vJavaGeneratedCallBacks.toString());

    // process virtual msgs
    fileSnr.put("%ackEnabled%", "false");
    for (String searchKey : fileSnr.keySet()) {
      virtualJava = virtualJava.replace(searchKey, fileSnr.get(searchKey));
    }

    // write out to files ..
    FileOutputStream MsgH = new FileOutputStream("src/resource/Arduino/MRLComm/Msg.h");
    FileOutputStream MsgCpp = new FileOutputStream("src/resource/Arduino/MRLComm/Msg.cpp");
    FileOutputStream MsgJava = new FileOutputStream("src/org/myrobotlab/arduino/Msg.java");
    FileOutputStream VirtualMsg = new FileOutputStream("src/org/myrobotlab/arduino/VirtualMsg.java");
    FileOutputStream ArduinoMsgCodedH = new FileOutputStream("src/resource/Arduino/MRLComm/ArduinoMsgCodec.h");

    ArduinoMsgCodedH.write(arduinoMsgCodeTemplateH.getBytes());
    MsgH.write(idlToHpp.getBytes());
    MsgCpp.write(idlToCpp.getBytes());
    MsgJava.write(idlToJava.getBytes());
    VirtualMsg.write(virtualJava.getBytes());

    ArduinoMsgCodedH.close();
    MsgH.close();
    MsgCpp.close();
    MsgJava.close();
    VirtualMsg.close();

  }

  public String IdlToCppType(String idlType) throws IOException {
    if (idlType.equals("b16")) {
      return "int";
    } else if (idlType.equals("bu16")) {
      return "unsigned int";
    } else if (idlType.equals("b32")) {
      return "long";
    } else if (idlType.equals("bu32")) {
      return "unsigned long";
    } else if (idlType.equals("str")) {
      return "char*";
    } else if (idlType.equals("[]")) {
      return "byte*";
    } else if (idlType.equals("bool")) {
      return "bool";
    } else if (idlType.equals("f32")) {
      return "float";
    } else if (idlType.equals("")) {
      return "byte";
    }

    throw new IOException(String.format("%s idlType unknown", idlType));
  }

  public String IdlToJavaType(String idlType) throws IOException {
    if (idlType.equals("b16")) {
      return "Integer";
    } else if (idlType.equals("bu16")) {
      return "Integer";
    } else if (idlType.equals("b32")) {
      return "Integer";
    } else if (idlType.equals("bu32")) {
      return "Long";
    } else if (idlType.equals("str")) {
      return "String";
    } else if (idlType.equals("[]")) {
      return "int[]";
    } else if (idlType.equals("bool")) {
      return "Boolean";
    } else if (idlType.equals("f32")) {
      return "Float";
    } else if (idlType.equals("")) {
      return "Integer";
    }

    throw new IOException(String.format("%s idlType unknown", idlType));
  }

  public int getCppTypeSize(String idlType) throws IOException {
    if (idlType.equals("b16")) {
      return 2;
    } else if (idlType.equals("bu16")) {
      return 2;
    } else if (idlType.equals("b32")) {
      return 4;
    } else if (idlType.equals("bu32")) {
      return 4;
    } else if (idlType.equals("b32")) {
      return 4;
    } else if (idlType.equals("bool")) {
      return 1;
    } else if (idlType.equals("")) {
      return 1;
    } else if (idlType.equals("f32")) {
      return 4;
    } else if (idlType.equals("str")) {
      return -1;
    } else if (idlType.equals("[]")) {
      return -1;
    }

    throw new IOException(String.format("%s idlType unknown size", idlType));
  }

  // per method per parameter
  public Map<String, String> perMsgMethod(int msgIndex, String line, char dir, String name, String[] paramaters) throws IOException {

    Map<String, String> methodSnr = new TreeMap<String, String>();

    // load templates
    String hMethod = toString("src/resource/Arduino/generate/Msg.method.template.h");
    String cppMethod = toString("src/resource/Arduino/generate/Msg.method.template.cpp");
    String javaMethod = toString("src/resource/Arduino/generate/Msg.method.template.java");

    // load search and replace
    Map<String, String> snr = new TreeMap<String, String>();
    snr.put("%name%", name);

    // MAGIC_NUMBER|MSG_SIZE|TYPE|PARM0|PARM1 ...
    StringBuilder cppWriteMsgSize = new StringBuilder("1"); // for size -
    // all msgs need
    // a type (1
    // byte)
    StringBuilder javaWriteMsgSize = new StringBuilder("1"); // for size -
    // all msgs
    // need a
    // type (1
    // byte)

    StringBuilder cppGeneratedCallBack = new StringBuilder("\t// " + line + "\n");
    cppGeneratedCallBack.append("\tvoid " + name + "(");

    StringBuilder define = new StringBuilder();
    define.append("// " + line + "\n");
    define.append("#define " + CodecUtils.toUnderScore(name) + " " + msgIndex + "\n");
    methodSnr.put("define", define.toString());

    StringBuilder javaDefine = new StringBuilder();
    javaDefine.append("\t// " + line + "\n");
    javaDefine.append("\tpublic final static int " + CodecUtils.toUnderScore(name) + " = " + msgIndex + ";\n");
    methodSnr.put("javaDefine", javaDefine.toString());

    StringBuilder javaGeneratedCallback = new StringBuilder("\t// public void " + name + "(");
    StringBuilder javaMethodParameters = new StringBuilder();
    StringBuilder cppMethodParameters = new StringBuilder();
    StringBuilder cppWrite = new StringBuilder("  write(" + CodecUtils.toUnderScore(name) + "); // msgType = " + msgIndex + "\n");
    StringBuilder javaWrite = new StringBuilder("\t\t\twrite(" + CodecUtils.toUnderScore(name) + "); // msgType = " + msgIndex + "\n");

    String arduinoOrMrlComm = (keywords.contains(name)) ? "" : "mrlComm->";
    StringBuilder cppCaseHeader = new StringBuilder("\tcase " + CodecUtils.toUnderScore(name) + ": { // " + name + "\n");
    StringBuilder cppCaseParams = new StringBuilder();
    StringBuilder cppCaseArduinoMethod = new StringBuilder("\t\t\t" + arduinoOrMrlComm + name + "(");

    StringBuilder javaCaseHeader = new StringBuilder("\t\tcase " + CodecUtils.toUnderScore(name) + ": {\n");
    StringBuilder javaCaseArduinoMethod = new StringBuilder("\t\t\tif(invoke){");
    javaCaseArduinoMethod.append("\n\t\t\t\tarduino.invoke(\"" + name + "\"");

    StringBuilder javaCaseRecord = new StringBuilder("\t\t\tif(record != null){");
    javaCaseRecord.append("\n\t\t\t\trxBuffer.append(\"< " + name + "\");\n");

    StringBuilder javaSendRecord = new StringBuilder("\t\t\tif(record != null){");
    javaSendRecord.append("\n\t\t\t\ttxBuffer.append(\"> " + name + "\");\n");

    // compiler check
    StringBuilder javaCaseArduinoMethodComment = new StringBuilder("\n\t\t\t} else { \n \t\t\t\tarduino." + name + "(");
    if (paramaters.length > 0) {
      javaCaseArduinoMethod.append(", ");
    }

    StringBuilder javaCaseParams = new StringBuilder();

    // ioCmd[1], ioCmd[2]
    String caseFooter = new String(");\n\t\t\tbreak;\n	}\n");
    String javaCaseFooter = new String("\n\t\t\tbreak;\n\t\t}\n");

    // PER PARAMETER
    // TODO deprecate
    int byteLocation = 1;

    for (int i = 0; i < paramaters.length; ++i) {
      String[] paramTypeAndName = paramaters[i].split(" ");
      String idlParamType = "";
      String paramName = "";

      if (paramTypeAndName.length > 1) {
        idlParamType = paramTypeAndName[0];
        paramName = paramTypeAndName[1];
      } else {
        paramName = paramTypeAndName[0];
      }

      String cppType = IdlToCppType(idlParamType);
      String javaType = IdlToJavaType(idlParamType);

      if (idlParamType.equals("str") || idlParamType.equals("[]")) {
        cppMethodParameters.append("const ");
      } else {
        cppMethodParameters.append(" ");
      }

      javaMethodParameters.append(javaType);
      javaMethodParameters.append(" ");
      String commentType = (idlParamType.equals("")) ? "byte" : idlParamType;
      javaMethodParameters.append(paramName + "/*" + commentType + "*/");

      cppMethodParameters.append(cppType);
      cppMethodParameters.append(" ");
      cppMethodParameters.append(paramName);

      if (idlParamType.equals("[]") || idlParamType.equals("str")) {
        cppMethodParameters.append(", ");
        cppMethodParameters.append(" byte " + paramName + "Size");
      }

      if (i != paramaters.length - 1) {
        cppMethodParameters.append(", ");
        javaMethodParameters.append(", ");
      }

      javaCaseRecord.append("\t\t\t\trxBuffer.append(\"/\");\n");
      if (idlParamType.equals("[]")) {
        javaCaseRecord.append("\t\t\t\trxBuffer.append(Arrays.toString(" + paramName + "));\n");
      } else {
        javaCaseRecord.append("\t\t\t\trxBuffer.append(" + paramName + ");\n");
      }

      javaSendRecord.append("\t\t\t\ttxBuffer.append(\"/\");\n");
      if (idlParamType.equals("[]")) {
        javaSendRecord.append("\t\t\t\ttxBuffer.append(Arrays.toString(" + paramName + "));\n");
      } else {
        javaSendRecord.append("\t\t\t\ttxBuffer.append(" + paramName + ");\n");
      }

      // msgSize += getCppTypeSize(idlParamType);

      // WRITE(..)
      if (idlParamType.equals("str")) {

        // cppWrite.append(" writestr(" + paramName + ");\n");
        cppWrite.append("  write((byte*)" + paramName + ", " + paramName + "Size);\n");
        javaWrite.append("\t\t\twrite(" + paramName + ");\n");

        javaWriteMsgSize.append(" + (1 + " + paramName + ".length())");

        // cppWriteMsgSize.append(" + (1 + strlen(" + paramName + "))");
        cppWriteMsgSize.append(" + (1 + " + paramName + "Size)");
      } else if (idlParamType.equals("[]")) {

        cppWrite.append("  write((byte*)" + paramName + ", " + paramName + "Size);\n");
        javaWrite.append("\t\t\twrite(" + paramName + ");\n");

        javaWriteMsgSize.append(" + (1 + " + paramName + ".length)");

        cppWriteMsgSize.append(" + (1 + " + paramName + "Size)");
      } else {
        cppWrite.append("  write" + idlParamType + "(" + paramName + ");\n");
        javaWrite.append("\t\t\twrite" + idlParamType + "(" + paramName + ");\n");
        cppWriteMsgSize.append(" + " + getCppTypeSize(idlParamType));
        javaWriteMsgSize.append(" + " + getCppTypeSize(idlParamType));
      }

      // recv case parameters
      if (idlParamType.equals("")) {
        cppCaseHeader.append("\t\t\tbyte " + paramName + " = ioCmd[startPos+1]; // bu8\n");
        cppCaseHeader.append("\t\t\tstartPos += 1;\n");
        cppCaseParams.append(" " + paramName);

        javaCaseHeader.append("\t\t\tInteger " + paramName + " = ioCmd[startPos+1]; // bu8\n");
        javaCaseHeader.append("\t\t\tstartPos += 1;\n");
        javaCaseParams.append(" " + paramName);

        cppGeneratedCallBack.append(" byte " + paramName);
        ++byteLocation;
      } else if (idlParamType.equals("bool")) {
        // cppCaseParams.append("(bool)ioCmd[" + byteLocation + "]");

        cppCaseHeader.append("\t\t\tboolean " + paramName + " = (ioCmd[startPos+1]);\n");
        cppCaseHeader.append("\t\t\tstartPos += 1;\n");
        cppCaseParams.append(" " + paramName);

        javaCaseHeader.append("\t\t\tBoolean " + paramName + " = (ioCmd[startPos+1] == 0)?false:true;\n");
        javaCaseHeader.append("\t\t\tstartPos += 1;\n");
        javaCaseParams.append(" " + paramName);

        cppGeneratedCallBack.append(" boolean " + paramName);
      } else if (idlParamType.equals("str")) {

        cppCaseHeader.append("\t\t\tconst char* " + paramName + " = (char*)ioCmd+startPos+2;\n");
        cppCaseHeader.append("\t\t\tbyte " + paramName + "Size = ioCmd[startPos+1];\n");
        cppCaseHeader.append("\t\t\tstartPos += 1 + ioCmd[startPos+1];\n");
        cppCaseParams.append(" " + paramName + "Size, " + paramName);

        // FIXME - this has to be done everywhere !!!!
        // PERHAPS USE javaTyeLocation as a String !!
        javaCaseHeader.append("\t\t\tString " + paramName + " = str(ioCmd, startPos+2, ioCmd[startPos+1]);\n");
        javaCaseHeader.append("\t\t\tstartPos += 1 + ioCmd[startPos+1];\n");
        javaCaseParams.append(" " + paramName);

        cppGeneratedCallBack.append(" byte " + paramName + "Size, const char*" + paramName);
        // ++byteLocation FIXME FIXME !!! - got to add a list of
        // variables !!!
        // there are 'fixed' and variable positions - variable position
        // is always last position - fixed is accumulated

      } else if (idlParamType.equals("[]")) {
        // FIXME - str better be at the end of the msg and only 1 -
        // if not this code will get more complicated !
        // the byteLocation will need a list of variables to offset

        // cppCaseParams.append("ioCmd[" + (byteLocation) + "] /*" +
        // paramName + "Size*/, (byte*)(ioCmd+" + (++byteLocation) +
        // ")");
        cppCaseHeader.append("\t\t\tconst byte* " + paramName + " = ioCmd+startPos+2;\n");
        cppCaseHeader.append("\t\t\tbyte " + paramName + "Size = ioCmd[startPos+1];\n");
        cppCaseHeader.append("\t\t\tstartPos += 1 + ioCmd[startPos+1];\n");
        cppCaseParams.append(" " + paramName + "Size, " + paramName);

        byteLocation += getCppTypeSize(idlParamType);

        javaCaseHeader.append("\t\t\tint[] " + paramName + " = subArray(ioCmd, startPos+2, ioCmd[startPos+1]);\n");
        javaCaseHeader.append("\t\t\tstartPos += 1 + ioCmd[startPos+1];\n");
        javaCaseParams.append(" " + paramName);

        cppGeneratedCallBack.append(" byte " + paramName + "Size, const byte*" + paramName);
        // ++byteLocation FIXME FIXME !!! - got to add a list of
        // variables !!!

      } else {
        // cppCaseParams.append(idlParamType + "(ioCmd + " +
        // byteLocation + ")");
        // javaCaseParams.append(idlParamType + "(ioCmd, startPos+1)");

        cppCaseHeader.append("\t\t\t" + cppType + " " + paramName + " = " + idlParamType + "(ioCmd, startPos+1);\n");
        cppCaseHeader.append("\t\t\tstartPos += " + getCppTypeSize(idlParamType) + "; //" + idlParamType + "\n");
        cppCaseParams.append(" " + paramName);

        // FIXME - change to Integer from int

        javaCaseHeader.append("\t\t\t" + javaType + " " + paramName + " = " + idlParamType + "(ioCmd, startPos+1);\n");
        javaCaseHeader.append("\t\t\tstartPos += " + getCppTypeSize(idlParamType) + "; //" + idlParamType + "\n");
        javaCaseParams.append(" " + paramName);

        byteLocation += getCppTypeSize(idlParamType);
        // javaByteLocation += getCppTypeSize(idlParamType);
        cppGeneratedCallBack.append(" " + cppType + " " + paramName);
      }

      if (i != paramaters.length - 1) {
        cppGeneratedCallBack.append(", ");
      }

      if (i != paramaters.length - 1) {
        cppCaseParams.append(",");
        javaCaseParams.append(",");
      }

      // cppCaseParams.append(" /*" + paramName + "*/");
      // javaCaseParams.append(" /*" + paramName + "*/");

      if (i != paramaters.length - 1) {
        // caseParams.append("\n\t\t");
        cppCaseParams.append(" ");
        javaCaseParams.append(" ");
      }

      // increment to the next pos FIXME (unless variable length) :P
      // ++byteLocation; // FIXME probably wrong !!
      // ++javaByteLocation;

    } // end parameter loop

    javaSendRecord.append("\t\t\t\ttxBuffer.append(\"\\n\");\n");
    javaSendRecord.append("\t\t\t\trecord.write(txBuffer.toString().getBytes());\n");
    javaSendRecord.append("\t\t\t\ttxBuffer.setLength(0);\n");
    javaSendRecord.append("\t\t\t}\n");

    javaCaseParams.append(");");
    // cpp
    cppGeneratedCallBack.append(");\n");
    if (keywords.contains(name)) {
      // no callback if an Arduino method
      cppGeneratedCallBack.setLength(0);
    }
    snr.put("%cppMethodParameters%", cppMethodParameters.toString());
    snr.put("%javaMethodParameters%", javaMethodParameters.toString());
    snr.put("%cppMsgSize%", "" + cppWriteMsgSize);
    snr.put("%javaWriteMsgSize%", "" + javaWriteMsgSize);

    snr.put("%cppWrite%", cppWrite.toString());
    snr.put("%javaWrite%", javaWrite.toString());
    snr.put("%javaSendRecord%", javaSendRecord.toString());    

    javaSendRecord.append("\t\t\t\trecord.write(txBuffer.toString().getBytes());\n");

    // process templates
    for (String search : snr.keySet()) {
      hMethod = hMethod.replace(search, snr.get(search));
      cppMethod = cppMethod.replace(search, snr.get(search));
      javaMethod = javaMethod.replace(search, snr.get(search));
    }

    javaCaseRecord.append("\t\t\trxBuffer.append(\"\\n\");\n");
    javaCaseRecord.append("\t\t\ttry{\n");
    javaCaseRecord.append("\t\t\t\trecord.write(rxBuffer.toString().getBytes());\n");
    javaCaseRecord.append("\t\t\t\trxBuffer.setLength(0);\n");
    javaCaseRecord.append("\t\t\t}catch(IOException e){}\n");

    // TODO
    if (dir == '<') {
      // send methods
      methodSnr.put("hMethod", hMethod);
      methodSnr.put("cppMethod", cppMethod);
      methodSnr.put("cppHandleCase", "");
      methodSnr.put("cppGeneratedCallBacks", "");

      methodSnr.put("javaHandleCase", javaCaseHeader.toString() + javaCaseArduinoMethod + javaCaseParams + javaCaseArduinoMethodComment + javaCaseParams + "\n\t\t\t}\n"
          + javaCaseRecord + "\t\t\t}\n" + javaCaseFooter);
      methodSnr.put("javaGeneratedCallBack", javaGeneratedCallback + javaMethodParameters.toString() + "){}\n");
      methodSnr.put("javaMethod", "");

      // vJava send methods
      methodSnr.put("vJavaMethod", javaMethod);
      methodSnr.put("vJavaHandleCase", "");
      methodSnr.put("vJavaGeneratedCallBack", "");

      
      
    } else {
      // cpp recv methods
      methodSnr.put("hMethod", "");
      methodSnr.put("cppMethod", "");
      methodSnr.put("cppHandleCase", cppCaseHeader.toString() + cppCaseArduinoMethod + cppCaseParams + caseFooter);
      methodSnr.put("cppGeneratedCallBacks", cppGeneratedCallBack.toString());

      // java send methods
      methodSnr.put("javaMethod", javaMethod);
      methodSnr.put("javaHandleCase", "");
      methodSnr.put("javaGeneratedCallBack", "");

      // vJava send methods
      methodSnr.put("vJavaHandleCase",
          javaCaseHeader.toString() + javaCaseArduinoMethod + javaCaseParams + javaCaseArduinoMethodComment + javaCaseParams + "\n\t\t\t}" + javaCaseFooter);
      methodSnr.put("vJavaGeneratedCallBack", javaGeneratedCallback + javaMethodParameters.toString() + "){}\n");
      methodSnr.put("vJavaMethod", "");
      
     // snr.put("%enableAck%", "enableAck(b);");
    }

    log.info("\n\n{}", hMethod);
    log.info("\n\n{}", cppMethod);

    return methodSnr;
  }

  static public void test(Integer x) {
    ++x;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // camelback to underscore
      /*
       * String regex = "[A-Z\\d]"; String replacement = "$1_";
       * log.info(t.replaceAll(regex, replacement)); log.info(t);
       */

      // log.info(CodecUtils.toUnderScore(t));
      // log.info(CodecUtils.toCamelCase(CodecUtils.toUnderScore(t)));
      ArduinoMsgGenerator generator = new ArduinoMsgGenerator();
      generator.generateDefinitions();

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
