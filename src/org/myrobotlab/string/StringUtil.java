package org.myrobotlab.string;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;

public class StringUtil {

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  static public String getLatestVersion(String[] versions) {

    if (versions == null || versions.length == 0) {
      return null;
    }

    // int[][] ver = new int[versions.length][3];

    int major = 0;
    int minor = 0;
    int build = 0;

    int latestMajor = 0;
    int latestMinor = 0;
    int latestBuild = 0;

    int latestIndex = 0;

    for (int i = 0; i < versions.length; ++i) {
      try {
        major = 0;
        minor = 0;
        build = 0;

        String[] parts = versions[i].split("\\.");
        major = Integer.parseInt(parts[0]);
        minor = Integer.parseInt(parts[1]);
        build = Integer.parseInt(parts[2]);
      } catch (Exception e) {
        // log.error(e.getMessage());
        System.out.println(e.getMessage());
      }

      if (major > latestMajor) {
        latestMajor = major;
        latestMinor = minor;
        latestBuild = build;
        latestIndex = i;
      } else if (major == latestMajor) {
        // go deeper (minor)
        if (minor > latestMinor) {
          latestMajor = major;
          latestMinor = minor;
          latestBuild = build;
          latestIndex = i;
        } else if (minor == latestMinor) {
          // go deeper (build)
          if (build > latestBuild) {
            latestMajor = major;
            latestMinor = minor;
            latestBuild = build;
            latestIndex = i;
          }
        }
      }
    }

    return versions[latestIndex];
  }

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static void main(String[] args) throws ClassNotFoundException {

    String methodName = StringUtil.StringToMethodName("hello all you freaks");
    methodName = StringUtil.StringToMethodName("thisIsATest over here");
    methodName = StringUtil.StringToMethodName("This would be a nifty method name");
    methodName = StringUtil.StringToMethodName("I have whitespace");

    methodName.toString();
  }

  public static String removeChar(String s, char c) {
    StringBuffer r = new StringBuffer(s.length());
    r.setLength(s.length());
    int current = 0;
    for (int i = 0; i < s.length(); i++) {
      char cur = s.charAt(i);
      if (cur != c)
        r.setCharAt(current++, cur);
    }
    return r.toString();
  }

  public static String removeCharAt(String s, int pos) {
    StringBuffer buf = new StringBuffer(s.length() - 1);
    buf.append(s.substring(0, pos)).append(s.substring(pos + 1));
    return buf.toString();
  }

  public static String replaceCharAt(String s, int pos, char c) {
    StringBuffer buf = new StringBuffer(s);
    buf.setCharAt(pos, c);
    return buf.toString();
  }

  public static String StringToMethodName(String english) {
    StringBuffer methodName = new StringBuffer();
    boolean afterWhitespace = false;
    for (int i = 0; i <= english.length() - 1; i++) {
      Character temp = english.charAt(i);
      if (temp != ' ') {
        if (i == 0) {
          temp = Character.toLowerCase(temp);
        } else if (afterWhitespace) {
          temp = Character.toUpperCase(temp);
        }

        methodName.append(temp);
        afterWhitespace = false;
      } else {
        afterWhitespace = true;
      }
    }

    return methodName.toString();
  }

  public static boolean isEmpty(String v) {
    // return true if the string is null or empty.
    if (v == null || "".equals(v)) {
      return true;
    }
    return false;
  }
  
  // split a string into sub strings that are a maxlength
  // also try to be smart about text so you don't break a word in 1/2
  public static List<String> chunkText(String text, int maxLength) {
    ArrayList<String> chunks = new ArrayList<String>();
    StringBuilder line = new StringBuilder();
    // TODO: smarter tokenization!
    // perhaps use a sentence splitter first, then 
    // split the sentence into multiple parts if necessary
    // to preserve a more natural boundary on the text.
    String[] parts = text.split(" ");
    for (String p : parts) {
      if (line.length() + p.length() >= maxLength) {
        // our substring is long enough.
        chunks.add(line.toString().trim());
        // reset out substring
        line = new StringBuilder();
        line.append(p);
      } else {
        // accumulate the words on the string.
        line.append(p);
        // TODO: this is currently hardcoded to be a space. not great, 
        // in theory it should be the
        // actual from the original string...
      }
      line.append(" ");
    }
    // add the last segment.
    chunks.add(line.toString().trim());
    return chunks; 
    
  }
  

  public static String removeAccents(String text) {
    if (text == null) {
      return null;
    } else {
      String clean =  Normalizer.normalize(text, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
      return clean;
    }
  }

  
  

}
