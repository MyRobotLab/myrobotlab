/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.audio.AudioProcessor;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.AudioData;
import org.slf4j.Logger;

/**
 * 
 * AudioFile - This service can be used to play an audio file such as an mp3.
 *
 */
public class AudioFile extends Service {
  static final long serialVersionUID = 1L;
  static final Logger log = LoggerFactory.getLogger(AudioFile.class);

  static public final String DEFAULT_TRACK = "default";

  // FIXME
  // skip(track)
  // pause(track)
  // continue(track)
  // silence()
  // play("http://blah.com/some.mpe")
  // FIXME - change String or URI filename to InputStream ..

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

  // FIXME - AudioProcessor is a bit weird looking not sure if the decodedFormat
  // stream is needed
  // FIXME -
  // https://stackoverflow.com/questions/12863081/how-do-i-get-mixer-channels-layout-in-java
  // support multiple mixers
  // FIXME - review -
  // https://stackoverflow.com/questions/25798200/java-record-mic-to-byte-array-and-play-sound
  //

  String currentTrack = DEFAULT_TRACK;
  transient Map<String, AudioProcessor> processors = new HashMap<String, AudioProcessor>();
  double volume = 1.0f;
  // if set to true, playback will become a no-op
  private boolean mute = false;

  public AudioFile(String n, String id) {
    super(n, id);
  }

  public void track(String trackName) {
    currentTrack = trackName;
    if (!processors.containsKey(trackName) || !processors.get(trackName).isRunning()) {
      log.info("starting new track {}", trackName);
      AudioProcessor processor = new AudioProcessor(this, trackName);
      processors.put(trackName, processor);
      processor.start();
    } else {
      // log.info("switching to track %s", trackName);
    }
  }
  
  @Override
  public void stopService() {
    super.stopService();
    for (AudioProcessor p : processors.values()) {
      p.stopPlaying();
      p.interrupt();
    }
  }

  // TODO test with jar://resource/AudioFile/tick.mp3 & https://host/mp3 :
  // localfile
  //
  public AudioData play(String filename) {

    if (filename == null || filename.isEmpty()) {
      error("asked to play a null filename!  error");
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

    if (data == null) {
      log.warn("asked to play a null AudioData!  error");
      return null;
    }
    if (data.track == null) {
      data.track = currentTrack;
    }
    track(data.track);
    processors.get(data.track).setVolume(volume);
    if (AudioData.MODE_QUEUED.equals(data.mode)) {
      // stick it on top of queue and let our default player play it
      return processors.get(data.track).add(data);

    } else if (AudioData.MODE_BLOCKING.equals(data.mode)) {
      return processors.get(data.track).play(data);
    }

    return data;
  }

  public AudioData playBlocking(String filename) {
    return playFile(filename, true);
  }

  public void pause() {
    processors.get(currentTrack).pause(true);// = true;
  }

  public void resume() {
    processors.get(currentTrack).pause(false);// .resume();
  }

  public void playFile(String filename) {
    playFile(filename, false);
  }

  public AudioData playFile(String filename, Boolean isBlocking) {

    if (filename == null) {
      log.warn("Asked to play a null file, sorry can't do that");
      return null;
    }

    File f = new File(filename);
    if (!f.exists()) {
      error("File not found to play back " + f.getAbsolutePath());
      log.warn("File not found to play back " + f.getAbsolutePath());
      return null;
    }

    AudioData data = new AudioData(filename);
    if (isBlocking) {
      data.mode = AudioData.MODE_BLOCKING;
    } else {
      data.mode = AudioData.MODE_QUEUED;
    }
    play(data);
    return data;
  }

  public void playFileBlocking(String filename) {
    playFile(filename, true);
  }

  public void playResource(String filename) {
    playResource(filename, false);
  };

  /**
   * plays a resource file - currently there are very few resource files - and that's how it should
   * be they are only used for demonstration/tutorial functionality
   * @param filename - name of file relative to the resource dir
   * @param isBlocking
   */
  public void playResource(String filename, Boolean isBlocking) {
    playFile(getResourceDir() + File.separator + filename, isBlocking);
  }

  public void silence() {
    // stop all tracks
    for (Map.Entry<String, AudioProcessor> entry : processors.entrySet()) {
      String key = entry.getKey();
      // processors.get(key).isPlaying = false;// <<- FIXME necessary ???
      processors.get(key).pause(true);
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
    this.volume = volume;
  }

  public void setVolume(double volume) {
    setVolume((float) volume);
  }

  public double getVolume() {
    return this.volume;
  }

  public String getTrack() {
    return currentTrack;
  }

  public void setVolume(float volume, String track) {
    track(track);
    setVolume(volume);
  }

  public AudioData waitFor(String filename, Object waitForMe) {
    AudioData data = new AudioData(filename);
    data.waitForLock = waitForMe;
    return data;
  }

  public void stop() {
    AudioProcessor ap = processors.get(currentTrack);
    // dump the current song
   
    // pause the next one if queued
    ap.pause(false); // FIXME me shouldn't it be true ?
    ap.stopPlaying();
  }

  // FIXME - implement ???
  public List<Object> getLocksWaitingFor(String queueName) {
    // TODO Auto-generated method stub
    return null;
  }

  public List<File> getFiles() {
    return getFiles(null, true);
  }

  public List<File> getFiles(String subDir, boolean recurse) {
    try {
      String dir = "audioFile" + File.separator + subDir;
      return FileIO.getFileList(dir, true);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return new ArrayList<File>();
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

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

      AudioFile audio = (AudioFile) Runtime.start("audio", "AudioFile");// robot1.getAudioFile();
      audio.play(new AudioData("https://ia802508.us.archive.org/5/items/testmp3testfile/mpthreetest.mp3"));

      MarySpeech robot1 = (MarySpeech) Runtime.start("robot1", "MarySpeech");

      AudioData data = new AudioData("whatHowCanYouSitThere.mp3");
      data.repeat = 4;
      data.track = "new track";
      // FIXME - need to compared scaled with range !!!! - inform others
      data.volume = 0.5;
      audio.play(data);

      audio.play("whatHowCanYouSitThere.mp3");
      audio.pause();
      audio.resume();

      // FIXME - audio.step() ??? .f() .ff() .b .bb() rewind(10)
      // .fastForward(10)

      log.info(audio.getTrack());

      audio.track("sound effects");
      log.info(audio.getTrack());
      audio.play("explosion.mp3");

      // audio.repeat("alert.mp3");

      audio.track();
      audio.play("sir.mp3");
      audio.play("bigdeal.mp3");
      audio.play("whatHowCanYouSitThere.mp3");

      audio.play("calmDown.mp3");
      audio.play("fightOff.mp3");
      audio.play("startTheAlarm.mp3");

      // audio.fadeOut("sound effects");

      // audio.waitForAll("alert");
      // audio.waitFor("alert", "default", waitToFinish);
      // audio.waitFor(waitingTrack, waitToFinish);
      // audio.repeat("alert.mp3");

      audio.track();
      // FIXME - implement audio.pause(1000); - also implement a "queued"
      // pause(1000);

      robot1.speakBlocking("ah no, you didn't really need to do that did you. you know how the klaxons hurt my ears");
      robot1.speakBlocking("get off your butt and doooo something");
      robot1.speakBlocking("i am going aft get a battle axe in the weapons locker");
      robot1.speakBlocking("by the time i get back, you had better be ready");
      robot1.speakBlocking("first thing i'm going to do is turn this thing down");
      audio.setVolume(0.20f, "sound effects"); // <-- name the robot's audio
      // file
      audio.track();
      robot1.speakBlocking("ahhh.. that's better - how can anyone think with that thing");

      audio.playBlocking("walking.mp3");
      robot1.speakBlocking("i mean it. Klaus");
      audio.playBlocking("door.mp3");

      robot1.speakBlocking("hello honey, what is your name");
      // mary.speak("hello my name is mary");
      robot1.speakBlocking("hi i'm Klaus - i think you sound a little like a robot");
      // mary.speak("yes, but i am open source");
      robot1.speakBlocking("can you dance like a robot too?");
      // mary.speak("i will try");

      // audio.setVolume(0.50f);
      robot1.speakBlocking("all right now she's gone its time to get funky");
      audio.playBlocking("scruff.mp3");
      robot1.speakBlocking("i need some snacks - i wonder if there is any left over chinese in the fridge");

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
        AudioFile player = (AudioFile)Runtime.start("player", "AudioFile"); // new AudioFile("player");
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
        player.playResource(Util.getResourceDir() + "/Clock/tick.mp3");
        player.playResource(Util.getResourceDir() + "/Clock/tick.mp3");
        player.playResource(Util.getResourceDir() + "/Clock/tick.mp3");
        player.playResource(Util.getResourceDir() + "/Clock/tick.mp3");
        player.playResource(Util.getResourceDir() + "/Clock/tick.mp3");
        player.playResource(Util.getResourceDir() + "/Clock/tick.mp3");
        player.playResource(Util.getResourceDir() + "/Clock/tick.mp3");
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
    // player.playBlockingWavFile("I am ready.wav");
    // player.play("hello my name is audery");
    // player.playWAV("hello my name is momo");
  }

  public AudioData repeat(String filename) {
    return repeat(filename, -1);
  }

  public AudioData repeat(String filename, Integer count) {
    // TODO Auto-generated method stub
    AudioData data = new AudioData(filename);
    data.repeat = count;
    return play(data);
  }

  public void track() {
    track(DEFAULT_TRACK);
  }

  public AudioData publishAudioStart(AudioData data) {
    return data;
  }

  public AudioData publishAudioEnd(AudioData data) {
    log.debug("Audio File publishAudioEnd");
    return data;
  }

  public void deleteFiles(String subDir) {
    // TODO Auto-generated method stub
    List<File> list = getFiles(subDir, true);
    for (File file : list) {
      try {
        file.delete();
      } catch (Exception e) {

      }
    }
  }

  public void deleteFile(String filename) {
    File file = new File(filename);
    file.delete();
  }

  public boolean isMute() {
    return mute;
  }

  public void setMute(boolean mute) {
    this.mute = mute;
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
    meta.addCategory("sound","music");

    meta.addDependency("javazoom", "jlayer", "1.0.1");
    meta.addDependency("com.googlecode.soundlibs", "mp3spi", "1.9.5.4");
    meta.addDependency("com.googlecode.soundlibs", "vorbisspi", "1.0.3.3"); // is
    // this
    // being
    // used
    // ?

    /*
     * meta.addDependency("javazoom.spi", "1.9.5");
     * meta.addDependency("javazoom.jl.player", "1.0.1");
     * meta.addDependency("org.tritonus.share.sampled.floatsamplebuffer",
     * "0.3.6");
     */
    return meta;
  }

}
