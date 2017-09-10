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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.audio.AudioProcessor;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

import com.amazonaws.services.config.model.GetResourceConfigHistoryRequest;

/**
 * 
 * AudioFile - This service can be used to play an audio file such as an mp3.
 *
 */
public class AudioFile extends Service {
  static final long serialVersionUID = 1L;
  static final Logger log = LoggerFactory.getLogger(AudioFile.class);

  static public final String DEFAULT_TRACK = "default";
  // FIXME -
  // http://www.javalobby.org/java/forums/t18465.html
  // http://sourcecodebrowser.com/libjlayer-java/1.0/classjavazoom_1_1jl_1_1player_1_1_audio_device_base__coll__graph.png
  // dataLine.getControls() returns this Master Gain with current value: 0.0 dB
  // (range: -80.0 - 6.0206)
  // Mute Control with current value: False Balance with current value: 0.0
  // (range: -1.0 - 1.0)
  // Pan with current value: 0.0 (range: -1.0 - 1.0) Ive been messing with my
  // code in the meantime and realized that the Master
  // Gains values IS actually changed but I can`t hear any cahnges in the song
  // http://alvinalexander.com/java/java-audio-example-java-au-play-sound - so
  // much more simple
  // http://www.programcreek.com/java-api-examples/index.php?api=javax.sound.sampled.FloatControl
  // http://stackoverflow.com/questions/198679/convert-audio-stream-to-wav-byte-array-in-java-without-temp-file
  // TODO - utilize
  // http://docs.oracle.com/javase/7/docs/api/javax/sound/sampled/Clip.html
  public static final String MODE_BLOCKING = "blocking";
  public static final String MODE_QUEUED = "queued";
  static String globalFileCacheDir = "audioFile";
  public static final String journalFilename = "journal.txt";
  // Map<String, BlockingQueue<AudioData>> tracks = new HashMap<String,
  // BlockingQueue<AudioData>>();// new
  String currentTrack = DEFAULT_TRACK;
  transient Map<String, AudioProcessor> processors = new HashMap<String, AudioProcessor>();
  // Map<String, Object> locks = new HashMap<String, Object>();
  Map<String, Object> waitForLocks = new HashMap<String, Object>();

  public AudioFile(String n) {
    super(n);
  }

  public void track(String trackName) {
    currentTrack = trackName;
    if (!processors.containsKey(trackName)) {
      AudioProcessor processor = new AudioProcessor(this, trackName);
      processors.put(trackName, processor);
      processor.start();
    }
  }

  // TODO test with jar://resource/AudioFile/tick.mp3 & https://host/mp3 :
  // localfile
  //
  public AudioData play(String filename) {
    if (filename == null) {
      log.warn("asked to play a null filename!  error");
      return null;
    }
    File f = new File(filename);
    if (!f.exists()) {
      log.warn("Tried to play file " + f.getAbsolutePath() + " but it was not found.");
      return null;
    }
    // use File interface such that filename is preserved
    // but regardless of location (e.g. url, local, resource)
    // or type (mp3 wav) a stream is opened and the
    // pair is put on a queue to be played

    // playFile("audioFile/" + filename + ".mp3", false);
    return play(new AudioData(filename));
  }

  public AudioData play(AudioData data) {
    // use File interface such that filename is preserved
    // but regardless of location (e.g. url, local, resource)
    // or type (mp3 wav) a stream is opened and the
    // pair is put on a queue to be played

    // make sure we are on
    // the currentTrack and its
    // created if necessary
    track(currentTrack);
    if (MODE_QUEUED.equals(data.mode)) {
      // stick it on top of queue and let our default player play it
      return processors.get(currentTrack).add(data);

    } else if (MODE_BLOCKING.equals(data.mode)) {
      return processors.get(currentTrack).play(data);
    }

    return data;
  }

  public void playBlocking(String filename) {
    playFile(filename, true);
  }

  public void pause() {
    processors.get(currentTrack).pause = true;
  }

  public void resume() {
    processors.get(currentTrack).pause = false;
    Object lock = processors.get(currentTrack).getLock();
    synchronized (lock) {
      lock.notify();
    }
  }

  public void playFile(String filename) {
    playFile(filename, false);
  }

  public void playFile(String filename, Boolean isBlocking) {

    if (filename == null) {
      log.warn("Asked to play a null file, sorry can't do that");
      return;
    }

    File f = new File(filename);
    if (!f.exists()) {
      log.warn("File not found to play back " + f.getAbsolutePath());
      return;
    }

    AudioData data = new AudioData(filename);
    if (isBlocking) {
      data.mode = AudioFile.MODE_BLOCKING;
    } else {
      data.mode = AudioFile.MODE_QUEUED;
    }
    play(data);
  }

  public void playFileBlocking(String filename) {
    playFile(filename, true);
  }

  public void playResource(String filename) {
    playResource(filename, false);
  };

  public void playResource(String filename, Boolean isBlocking) {    
    log.warn("Audio File playResource not implemented yet.");
    // FIXME AudioData needs to have an InputStream !!! 
    // cheesy - should play resource from the classpath
    playFile(filename, isBlocking);
    // TODO: what/who uses this? should we use the class loader to
    // playFile(filename, isBlocking, true);
    
  }

  public void silence() {
    // stop all tracks
    for (Map.Entry<String, AudioProcessor> entry : processors.entrySet()) {
      String key = entry.getKey();
      processors.get(key).isPlaying = false;
      processors.get(key).pause = true;
      // do what you have to do here
      // In your case, an other loop.
    }
  }

  /*
   * Specify the volume for playback on the audio file value 0.0 = off 1.0 =
   * normal volume. (values greater than 1.0 may distort the original signal)
   * 
   */
  public void setVolume(float volume) {
    processors.get(currentTrack).setVolume(volume);
  }

  public void setVolume(double volume) {
    processors.get(currentTrack).setVolume((float) volume);
  }

  public float getVolume() {
    return processors.get(currentTrack).getVolume();
  }

  public boolean cacheContains(String filename) {
    File file = new File(globalFileCacheDir + File.separator + filename);
    return file.exists();
  }

  public AudioData playCachedFile(String filename) {
    return play(globalFileCacheDir + File.separator + filename);
  }

  public void cache(String filename, byte[] data, String toSpeak) throws IOException {
    File file = new File(globalFileCacheDir + File.separator + filename);
    File parentDir = new File(file.getParent());
    if (!parentDir.exists()) {
      parentDir.mkdirs();
    }
    FileOutputStream fos = new FileOutputStream(globalFileCacheDir + File.separator + filename);
    fos.write(data);
    fos.close();
    // Now append the journal entry to the journal.txt file
    FileWriter journal = new FileWriter(globalFileCacheDir + File.separator + journalFilename, true);
    journal.append(filename + "," + toSpeak + "\r\n");
    journal.close();
  }

  public static String getGlobalFileCacheDir() {
    return globalFileCacheDir;
  }

  public String getTrack() {
    return currentTrack;
  }

  public void setVolume(float volume, String track) {
    track(track);
    setVolume(volume);
  }

  // does this work ?
  public AudioData repeat(String filename, String track) {
    track(track);
    repeat();
    return play(filename);
  }
  /*
   * TODO IMPLEMENT ??? private AudioProcessor getOrCreateTrack(String track){
   * 
   * }
   * 
   */

  // vs waitFor(String track, String waitingOn)
  public void waitFor(String waitingTrack, String track, AudioData waitForMe) {

    AudioProcessor processor = null;
    if (!processors.containsKey(waitingTrack)) {
      processor = new AudioProcessor(this, waitingTrack);
      processors.put(waitingTrack, processor);
    } else {
      processor = processors.get(waitingTrack);
    }

    processors.get(waitingTrack).waitForKey = waitForMe.toString();

    // processors.get(currentTrack)
    /*
     * synchronized (locks.get(currentTrack)) { locks.get(currentTrack).wait();
     * }
     */
    if (!processor.isRunning()) {
      processor.start();
    }

  }

  public Object getWaitForLock(String key) {
    if (waitForLocks.containsKey(key)) {
      return waitForLocks.get(key);
    }
    return null;
  }

  public void repeat() {
    // TODO Auto-generated method stub
    processors.get(currentTrack).repeat = true;
  }

  public void stop() {
    // dump the current song
    processors.get(currentTrack).isPlaying = false;
    // pause the next one if queued
    processors.get(currentTrack).pause = true;
  }

  /*
   * public AudioData getNextAudioData(String track) throws InterruptedException
   * { return tracks.get(track).take(); }
   */
  /*
   * public Object getLock(String track) { return locks.get(track); }
   */
  public List<Object> getLocksWaitingFor(String queueName) {
    // TODO Auto-generated method stub
    return null;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);

    try {

      // FIXME
      // TO - TEST
      // file types mp3 wav
      // file locations http resource jar! file cache !!!
      // basic controls - playMulti, playQueue, playBlocking

      // FIXME - setting volume on a non-played track null pointer
      // FIXME - DNA make single AudioFile
      // AudioFile audio = (AudioFile) Runtime.createAndStart("audio",
      // "AudioFile");
      // MarySpeech mary = (MarySpeech) Runtime.start("mary", "MarySpeech");

      NaturalReaderSpeech robot1 = (NaturalReaderSpeech) Runtime.start("robot1", "NaturalReaderSpeech");
      AudioFile audio = robot1.getAudioFile();

      log.info(audio.getTrack());

      // audio.track("sound effects");
      audio.track("explosion");
      audio.play("alert.mp3");

      /*
       * mary.speak("warning warning. danger will robinson danger"); mary.speak(
       * "yes i might not sound quite as sexy as the other speech services but i am open source"
       * ); mary.speak("and they can do things like this");
       */

      // FIXME all use the same single AudioFile
      // basic dialog - they are all on the same track so stacked up and
      // interleaved
      // audio.track(DEFAULT_TRACK); // FIXME - Acapela - needs to create robot1
      // Track !!
      audio.track(); // FIXME - Acapela - needs to create robot1 Track !!
      robot1.speak("Rhona", "sir.. I believe we are under attack from inter-bots");
      robot1.speak("Tyler", "big deal, its just a bunch of inter-bots..  i really dont think it is a problem... I mean we are");
      robot1.speak("Rhona", "what? how can you just sit there on your metal butt and do nothing");
      robot1.speak("Tyler", "calm down - please just calm down");
      robot1.speak("Rhona", "fine, some one needs to take care of this. Im going to fight them off with my bare hands");
      AudioData alarm = robot1.speak("Rhona", "i better start the alarm and warn the rest of the crew");

      robot1.speakBlocking("I am testing blocking on speech");

      /// audio.repeat("flybye.mp3","flybye");

      // FIXME - implement - audio.waitFor(alarm);
      // audio.waitFor("alert", "default");
      // audio.waitForAll("alert");
      audio.waitFor("alert", "default", alarm);
      audio.repeat("alert.mp3", "alert");

      audio.track("default");
      // FIXME - implement audio.pause(1000); - also implement a "queued"
      // pause(1000);

      robot1.speak("Tyler", "ahh noooooo, you didn't really need to do that did you. you know how the klaxons give me a headache");
      robot1.speak("Rhona", "get off your butt and doooo something");
      robot1.speak("i am going aft get a battle axe in the weapons locker");
      robot1.speak("by the time i get back, you had better be ready");
      robot1.speak("Tyler", "first thing i'm going to do is turn this thing down");
      audio.setVolume(0.20f, "alert"); // <-- name the robot's audio file
      audio.track("default");
      robot1.speak("ahhh.. that's better - how can anyone think with that thing");

      audio.play("walking.mp3");
      robot1.speak("Rhona", "i mean it. tyler");
      audio.play("door.mp3");

      robot1.speak("Tyler", "hello honey, what is your name");
      // mary.speak("hello my name is mary");
      robot1.speak("hi i'm Tyler - i think you sound a little like a robot");
      // mary.speak("yes, but i am open source");
      robot1.speak("can you dance like a robot too?");
      // mary.speak("i will try");

      // audio.setVolume(0.50f);
      robot1.speak("all right now she's gone its time to get funky");
      audio.play("scruff.mp3");
      robot1.speak("i need some snacks - i wonder if there is any left over chinese in the fridge");

      // turn the klaxons down - turn the music up

      // another woman

      // establishing a priority queue
      audio.track("priority");
      audio.play("alert.mp3");

      audio.setVolume(0.50f);
      audio.setVolume(0.20f);

      audio.play("nature.wav");
      audio.pause();

      audio.resume();
      audio.pause();
      audio.resume();
      audio.silence();
      // audio.fadeOut() // fadeOut is fadeOutAll
      // audio.fadeOutAll() fadeOut(track) - time option too
      // audio.fadeIn() //
      // audio.fadeInAll()

      // audio.setAllVolume();
      // af.playFile("C:\\dev\\workspace.kmw\\myrobotlab\\test.mp3",
      // false, false);

      audio.stop();
      audio.resume();

      boolean test = false;
      if (test) {
        audio.silence();

        Joystick joystick = (Joystick) Runtime.createAndStart("joy", "Joystick");
        Runtime.createAndStart("python", "Python");
        AudioFile player = new AudioFile("player");
        // player.playFile(filename, true);
        player.startService();
        Runtime.createAndStart("gui", "SwingGui");

        joystick.setController(2);
        joystick.broadcastState();

        // BasicController control = (BasicController) player;

        player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\thump.mp3");
        player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\thump.mp3");
        player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\thump.mp3");
        player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\start.mp3");
        player.playFile("C:\\Users\\grperry\\Downloads\\soapBox\\radio.chatter.4.mp3");

        player.silence();

        // player.playResource("Clock/tick.mp3");
        player.playResource("/resource/Clock/tick.mp3");
        player.playResource("/resource/Clock/tick.mp3");
        player.playResource("/resource/Clock/tick.mp3");
        player.playResource("/resource/Clock/tick.mp3");
        player.playResource("/resource/Clock/tick.mp3");
        player.playResource("/resource/Clock/tick.mp3");
        player.playResource("/resource/Clock/tick.mp3");
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
    // player.playBlockingWavFile("I am ready.wav");
    // player.play("hello my name is audery");
    // player.playWAV("hello my name is momo");
  }

  public void track() {
    track(DEFAULT_TRACK);
  }

  public String finishedPlaying(String utterance) {
    // TODO: maybe wire though the utterance?
    log.info("Finished playing called");
    return utterance;
  }

  public AudioData publishAudioStart(AudioData data) {
    return data;
  }

  public AudioData publishAudioEnd(AudioData data) {
    log.info("Audio File publishAudioEnd");
    return data;
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

    ServiceType meta = new ServiceType(AudioFile.class.getCanonicalName());
    meta.addDescription("can play audio files on multiple tracks");
    meta.addCategory("sound");
    meta.addDependency("javazoom.spi", "1.9.5");
    meta.addDependency("javazoom.jl.player", "1.0.1");
    meta.addDependency("org.tritonus.share.sampled.floatsamplebuffer", "0.3.6");
    return meta;
  }

}
