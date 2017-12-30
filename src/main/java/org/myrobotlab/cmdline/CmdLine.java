/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.cmdline;

import java.util.ArrayList;
import java.util.HashMap;

public class CmdLine extends HashMap<String, CcmdParam> {

  private static final long serialVersionUID = 1560723637806853945L;

  private String[] args = null;

  public static void main(String[] args) {

    CmdLine cmdline = new CmdLine(args);

    if (cmdline.containsKey("-test")) {
      String service = cmdline.getSafeArgument("-service", 0, "");
      System.out.println("Service: " + service);
    }
  }

  public CmdLine(String[] args) {
    splitLine(args);
  }

  public String getArgument(final String pSwitch, int iIdx) {
    if (hasSwitch(pSwitch)) {
      if (containsKey(pSwitch)) {
        if (get(pSwitch).m_strings.size() > iIdx) {
          return get(pSwitch).m_strings.get(iIdx);
        }
      }
    }

    // throw (int)0;

    return "";

  }

  public int getArgumentCount(final String pSwitch) {
    int iArgumentCount = -1;

    if (hasSwitch(pSwitch)) {
      if (containsKey(pSwitch)) {
        iArgumentCount = get(pSwitch).m_strings.size();
      }
    }

    return iArgumentCount;
  }

  public ArrayList<String> getArgumentList(final String pSwitch) {
    return get(pSwitch).m_strings;
  }

  public String getSafeArgument(final String pSwitch, int iIdx, final String pDefault) {
    String sRet = new String("");

    if (pDefault != null) {
      sRet = pDefault;
    }

    if (!hasSwitch(pSwitch))
      return sRet;

    String r = getArgument(pSwitch, iIdx);
    if ((r == null || r.length() == 0) && (pDefault != null && pDefault.length() != 0)) {
      return pDefault;
    } else {
      return r;
    }
  }

  public boolean hasSwitch(final String pSwitch) {
    return containsKey(pSwitch);
  }

  public boolean isSwitch(final String pParam) {
    if (pParam == null) {
      return false;
    }
    if (pParam.length() <= 1) {
      return false;
    }

    if (pParam.charAt(0) == '-') {
      boolean ret = true;

      // allow negative numbers as arguments.
      // ie., don't count them as switches
      ret &= !Character.isDigit(pParam.charAt(1));

      // if we have a space then the param was escaped
      // if its escaped e.g. -agent "-test -logLevel WARN" then its not a
      // flag
      ret &= !pParam.contains(" ");
      return ret;
    } else {
      return false;
    }

  }

  public int splitLine(String[] args) {
    // HashMap<String, CcmdParam> a = new HashMap<String, CcmdParam>();
    // a.put(arg0, arg1)
    this.args = args;
    String curParam = new String();
    for (int i = 0; i < args.length; ++i) {
      if (isSwitch(args[i])) {
        curParam = args[i];
        String arg = "";

        // look at next input string to see if it's a switch or an
        // argument
        if (i + 1 < args.length) {
          if (!isSwitch(args[i + 1])) {
            // it's an argument, not a switch
            arg = args[i + 1];

            // skip to next
            i++;
          } else {
            arg = "";
          }
        }

        // add it
        CcmdParam cmd = new CcmdParam();

        // only add non-empty args
        if (arg != "") {
          cmd.m_strings.add(arg);
        }

        // add the CCmdParam to 'this'
        put(curParam, cmd);
      } else {
        // it's not a new switch, so it must be more stuff for the last
        // switch
        // ...let's add it
        // get an iterator for the current param
        if (containsKey(curParam)) {
          // (*theIterator).second.m_strings.push_back(argv[i]);
          get(curParam).m_strings.add(args[i]);
        } else {
          // ??
        }
      }
    }
    return 5;

  }

  @Override
  public String toString() {
    StringBuffer ret = new StringBuffer();
    for (int i = 0; i < args.length; ++i) {
      ret.append("\"" + args[i] + "\"").append(" ");
    }

    return ret.toString();
  }
}
