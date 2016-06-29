package org.myrobotlab.roomba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RTTTLParser {

  public static HashMap<String, Integer> noteToNum;

  static {
    noteToNum = new HashMap<String, Integer>();
    noteToNum.put("c", new Integer(0));
    noteToNum.put("c#", new Integer(1));
    noteToNum.put("d", new Integer(2));
    noteToNum.put("d#", new Integer(3));
    noteToNum.put("e", new Integer(4));
    noteToNum.put("f", new Integer(5));
    noteToNum.put("f#", new Integer(6));
    noteToNum.put("g", new Integer(7));
    noteToNum.put("g#", new Integer(8));
    noteToNum.put("a", new Integer(9));
    noteToNum.put("a#", new Integer(10));
    noteToNum.put("b", new Integer(11));
    noteToNum.put("h", new Integer(7));
  }

  public static int bpmToMillis(int bpm) {
    return (60 * 1000) / bpm;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("usage: roombacomm.RTTTLParser <rttlstring>");
      System.exit(0);
    }
    String rtttl = args[0];
    ArrayList<Note> notelist = parse(rtttl);
    for (int i = 0; i < notelist.size(); i++) {
      System.out.println("notelist[" + i + "]=" + notelist.get(i));
    }
  }

  public static ArrayList<Note> parse(String rtttl) {
    System.out.println("parsing: " + rtttl);
    String rtttl_working = rtttl.toLowerCase();
    String parts[] = rtttl_working.split(":");
    // String name = parts[0];
    String defaults[] = parts[1].split("[,=]");
    String notes[] = parts[2].split(",");

    // global defaults
    int bpm = 63;
    int octave = 6;
    int duration = 4;

    ArrayList<Note> notelist = new ArrayList<Note>();

    for (int i = 0; i < defaults.length; i++) {
      // System.out.println("defaults["+i+"]="+defaults[i]);
      if (defaults[i].equals("b"))
        try {
          bpm = Integer.parseInt(defaults[i + 1]);
        } catch (Exception e) {
        }
      else if (defaults[i].equals("o"))
        try {
          octave = Integer.parseInt(defaults[i + 1]);
        } catch (Exception e) {
        }
      else if (defaults[i].equals("d"))
        try {
          duration = Integer.parseInt(defaults[i + 1]);
        } catch (Exception e) {
        }
    }
    System.out.println("bpm:" + bpm + ",octave:" + octave + ",duration:" + duration);

    for (int i = 0; i < notes.length; i++) {
      Matcher m = Pattern.compile("(\\d+)*(.+?)(\\d)*(\\.)*").matcher(notes[i]);
      m.find();
      // group(1) == duration (optional)
      // group(2) == note (required)
      // group(3) == scale (optional)
      // group(4) == triplet (optional)
      int dur = duration;
      int oct = octave;
      if (m.group(1) != null)
        try {
          dur = Integer.parseInt(m.group(1));
        } catch (Exception e) {
        }
      if (m.group(4) != null && m.group(4).equals("."))
        dur += dur / 2;
      if (m.group(3) != null)
        try {
          oct = Integer.parseInt(m.group(3));
        } catch (Exception e) {
        }
      if (m.group(2) != null) {
        int notenum;
        if (m.group(2).equals("p")) {
          notenum = 0;
        } else {
          Integer nn = (Integer) noteToNum.get(m.group(2));
          notenum = nn.intValue();
          notenum = notenum + 12 * oct;
        }
        dur = bpmToMillis(bpm) / dur;
        notelist.add(new Note(notenum, dur));
      }
    }
    return notelist;
  }
}
