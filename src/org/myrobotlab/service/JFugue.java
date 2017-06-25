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

package org.myrobotlab.service;

import org.jfugue.player.Player;
import org.jfugue.rhythm.Rhythm;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * JFugue - This service can generate tones to be played Also it can generate
 * some sounds and music based on string patterns that define the beat.
 *
 */
public class JFugue extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(JFugue.class);
  // transient public ManagedPlayer player = new ManagedPlayer();
  transient public Player player = new Player();

  // TODO - look at JavaSoundDemo - they have a synth & mixer there

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);
    JFugue jfugue = (JFugue) Runtime.start("jfugue", "JFugue");
    jfugue.play("C");
    jfugue.playRythm("O..oO...O..oOO..");
    jfugue.play("C");
    jfugue.play("C7h");
    jfugue.play("C5maj7w");
  }

  public JFugue(String n) {
    super(n);
  }

  public void play(Integer i) { // play tone
    // player.play("[A" + i + "]w");
    player.play("[" + i + "]");
  }

  public void play(Rhythm rythm) {
    player.play(rythm);
  }

  public void play(String s) {
    player.play(s);
  }

  public void playRythm(String data) {
    Rhythm rhythm = new Rhythm();
    rhythm.addLayer(data);
    player.play(rhythm.getPattern());
    /*
     * rhythm.setLayer(1, "O..oO...O..oOO.."); rhythm.setLayer(2,
     * "..*...*...*...*."); rhythm.addSubstitution('O', "[BASS_DRUM]i");
     * rhythm.addSubstitution('o', "Rs [BASS_DRUM]s");
     * rhythm.addSubstitution('*', "[ACOUSTIC_SNARE]i");
     * rhythm.addSubstitution('.', "Ri");
     * 
     * play(rhythm);
     */
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(JFugue.class.getCanonicalName());
    meta.addDescription("service wrapping Jfugue, used for music and sound generation");
    meta.addCategory("sound");
    meta.addDependency("org.jfugue.music", "5.0");
    return meta;
  }



}
