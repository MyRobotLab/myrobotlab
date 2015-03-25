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

import org.jfugue.Player;
import org.jfugue.Rhythm;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class JFugue extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(JFugue.class.getCanonicalName());
	transient public Player player = new Player();

	// TODO - look at JavaSoundDemo - they have a synth & mixer there

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

	}

	public JFugue(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "sound" };
	}

	@Override
	public String getDescription() {
		return "service wrapping Jfugue - http://www.jfugue.org/ used for music and sound generation";
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

	public void playRythm() {
		Rhythm rhythm = new Rhythm();
		rhythm.setLayer(1, "O..oO...O..oOO..");
		rhythm.setLayer(2, "..*...*...*...*.");
		rhythm.addSubstitution('O', "[BASS_DRUM]i");
		rhythm.addSubstitution('o', "Rs [BASS_DRUM]s");
		rhythm.addSubstitution('*', "[ACOUSTIC_SNARE]i");
		rhythm.addSubstitution('.', "Ri");
		play(rhythm);
	}

	@Override
	public void stopService() {
		if (player.isPlaying()) {
			player.stop();
		}
		player.close();
	}

	@Override
	public Status test() {
		Status status = super.test();

		JFugue jfugue = (JFugue) Runtime.start(getName(), getSimpleName());
		jfugue.play("C");
		jfugue.play("C7h");
		jfugue.play("C5maj7w");
		jfugue.play("G5h+B5h+C6q_D6q");
		jfugue.play("G5q G5q F5q E5q D5h");
		jfugue.play("T[Allegro] V0 I0 G6q A5q V1 A5q G6q");
		jfugue.play("V0 Cmajw V1 I[Flute] G4q E4q C4q E4q");
		jfugue.play("T120 V0 I[Piano] G5q G5q V9 [Hand_Clap]q Rq");

		jfugue.play("C3w D6h E3q F#5i Rs Ab7q Bb2i");
		jfugue.play("I[Piano] C5q D5q I[Flute] G5q F5q");
		jfugue.play("V0 A3q B3q C3q B3q V1 A2h C2h");
		jfugue.play("Cmaj5q F#min2h Bbmin13^^^");

		jfugue.play(30);
		jfugue.play(32);
		jfugue.play(44);
		jfugue.play(90);
		jfugue.play("A");

		jfugue.playRythm();

		jfugue.play("C D E F G A B");
		jfugue.play("A A A B B B");
		jfugue.playRythm();
		jfugue.play(30);
		jfugue.play(31);
		jfugue.play(40);
		jfugue.play(55);
		jfugue.play("E5s A5s C6s B5s E5s B5s D6s C6i E6i G#5i E6i | A5s E5s A5s C6s B5s E5s B5s D6s C6i A5i Ri");
		jfugue.play(55);

		return status;
	}

}
