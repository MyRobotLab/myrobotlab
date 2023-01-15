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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.audio.AudioProcessor;
import org.myrobotlab.audio.PlaylistPlayer;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.config.AudioFileConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.interfaces.AudioPublisher;
import org.slf4j.Logger;

/**
 * 
 * AudioFile - This service can be used to play an audio file such as an mp3.
 * 
 * TODO - publishPeak interface
 *
 */
public class AudioFile extends Service implements AudioPublisher {
  static final long serialVersionUID = 1L;
  static final Logger log = LoggerFactory.getLogger(AudioFile.class);

  public static final String DEFAULT_TRACK = "default";

  // statically initialize the supported files list.
  protected static Set<String> supportedFiles;
  static {
    supportedFiles = new HashSet<>();
    supportedFiles.add("mp3");
    supportedFiles.add("wav");
    supportedFiles.add("ogg");
    supportedFiles.add("flac");
    supportedFiles.add("aiff");
    supportedFiles.add("raw");
  }

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

  /**
   * Volume range 0 to 1.0. Over 1.0 can cause distortions
   */
  double volume = 1.0f;

  /**
   * mute - is reading through the audiofile and blocking or effectively reading the file
   * but not outputing to the sound line.
   */
  private boolean mute = false;

  protected String currentPlaylist = "default";
  
  /**
   * The currently played audio data - or the last activated
   * since AudioFile can play multiple tracks simultaneously.
   * The "latest" played AudioData - should be playing 
   */
  protected AudioData current = null; 
  
  /**
   * the last AudioData to be played
   */
  protected AudioData lastPlayed = null;

  protected Map<String, List<String>> playlists = new HashMap<>();

  final private transient PlaylistPlayer playlistPlayer = new PlaylistPlayer(this);

  protected double peakMultiplier = 1.0;

  public double getPeakMultiplier() {
    return peakMultiplier;
  }

  public void setPeakMultiplier(double peakMultiplier) {
    this.peakMultiplier = peakMultiplier;
  }

  public AudioFile(String n, String id) {
    super(n, id);
  }

  @Deprecated /* use setTrack */
  public void track(String trackName) {
    setTrack(trackName);
  }

  public void setTrack(String trackName) {
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

  public AudioData play(String filename) {
    return play(filename, false);
  }

  public AudioData play(String filename, boolean blocking) {
    return play(filename, blocking, null, null);
  }

  public AudioData play(String filename, boolean blocking, Integer repeat, String track) {

    if (track == null || track.isEmpty()) {
      track = currentTrack;
    }

    if (filename == null || filename.isEmpty()) {
      error("asked to play a null filename!  error");
      return null;
    }

    if (filename.toLowerCase().startsWith("http:") || filename.toLowerCase().startsWith("https:")) {
      // make cache directory if it doesn't exist
      File check = new File(getDataDir() + fs + "cache");
      if (!check.exists()) {
        check.mkdirs();
      }

      // check if cache file already exists - if so play it
      File cacheFile = new File(getDataDir() + fs + "cache" + fs + FileIO.cleanFileName(filename));
      if (cacheFile.exists()) {
        filename = cacheFile.getAbsolutePath();
      } else {
        // cache file doesn't exists - download it and save it to cache if valid
        byte[] data = Http.get(filename);
        if (data == null || data.length == 0) {
          error("could not fetch %s", filename);
          return null;
        }
        try {
          FileIO.toFile(cacheFile, data);
          filename = cacheFile.getAbsolutePath();
        } catch (Exception e) {
          error(e);
        }
      }
    } // end of http

    File f = new File(filename);
    if (!f.exists()) {
      error("tried to play file " + f.getAbsolutePath() + " but it was not found.");
      return null;
    }

    AudioData data = new AudioData(filename);
    data.mode = blocking ? AudioData.MODE_BLOCKING : AudioData.MODE_QUEUED;
    data.track = track;
    data.repeat = repeat;

    return play(data);
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
    setTrack(data.track);    

    if (AudioData.MODE_QUEUED.equals(data.mode)) {
      // stick it on top of queue and let our default player play it
      return processors.get(data.track).add(data);

    } else if (AudioData.MODE_BLOCKING.equals(data.mode)) {
      return processors.get(data.track).play(data);
    }

    return data;
  }

  public AudioData playBlocking(String filename) {
    return play(filename, true);
  }

  public void pause() {
    processors.get(currentTrack).pause(true);// = true;
  }

  public void resume() {
    processors.get(currentTrack).pause(false);// .resume();
  }

  @Deprecated /* use play */
  public void playFile(String filename) {
    play(filename);
  }

  @Deprecated /* use play */
  public AudioData playFile(String filename, Boolean isBlocking) {
    return play(filename, isBlocking);
  }

  @Deprecated /* use playBlocking */
  public void playFileBlocking(String filename) {
    play(filename, true);
  }

  public void playResource(String filename) {
    playResource(filename, false);
  };

  /**
   * plays a resource file - currently there are very few resource files - and
   * that's how it should be they are only used for demonstration/tutorial
   * functionality
   * 
   * @param filename
   *          - name of file relative to the resource dir
   * @param isBlocking
   *          if the playback should block
   */
  public void playResource(String filename, Boolean isBlocking) {
    play(getResourceDir() + File.separator + filename, isBlocking);
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
    playlistPlayer.stop();
  }

  /**
   * Specify the volume for playback on the audio file value 0.0 = off 1.0 =
   * normal volume. (values greater than 1.0 may distort the original signal)
   * 
   * @param volume
   */
  public void setVolume(float volume) {
    if (volume > 1.0) {
      error("volume must be in a range between 0.0 and 1.0");
      return;
    }
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
    setTrack(track);
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

  public AudioData repeat(String filename) {
    return repeat(filename, -1);
  }

  public AudioData repeat(String filename, Integer count) {
    // TODO Auto-generated method stub
    AudioData data = new AudioData(filename);
    data.repeat = count;
    return play(data);
  }

  @Deprecated /* use setTrack() */
  public void track() {
    setTrack(DEFAULT_TRACK);
  }

  public void setTrack() {
    setTrack(DEFAULT_TRACK);
  }

  @Override
  public AudioData publishAudioStart(AudioData data) {
    current = data;
    return data;
  }

  @Override
  public AudioData publishAudioEnd(AudioData data) {
    log.debug("Audio File publishAudioEnd");
    current = null;
    lastPlayed = data;
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

  public void setPlaylist(String name) {
    currentPlaylist = name;
  }

  public void addPlaylist(String folderPath) {
    addPlaylist(currentPlaylist, folderPath);
  }

  public void addPlaylist(String name, String path) {

    List<String> list = null;
    if (!playlists.containsKey(name)) {
      list = new ArrayList<String>();
    } else {
      list = playlists.get(name);
    }
    File check = new File(path);
    if (!check.exists()) {
      error("%s playlist folder or file %s does not exist", name, path);
      return;
    }
    if (check.isDirectory()) {
      list.addAll(scanForMusicFiles(path));
    }
    int filecount = list.size();
    playlists.put(name, list);
    log.info("{} playlist added {} files", name, filecount);
  }

  private List<String> scanForMusicFiles(String path) {
    // scan directory or add file
    Stream<Path> walk;
    try {
      walk = Files.walk(Paths.get(path));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.warn("Unable to walk file path {}", path, e);
      return null;
    }

    // make sure it's a file and that we can read it
    List<File> result = walk.map(f -> f.toFile()).filter(f -> f.isFile()).filter(f -> f.canRead())
        .filter(f -> supportedFiles.contains(StringUtils.lowerCase(FilenameUtils.getExtension(f.getName()))))
        // .filter(f -> f.getTotalSpace() > 0)
        .collect(Collectors.toList());
    List<String> playlist = new ArrayList<String>();
    for (File file : result) {
      String absFilePath = file.getAbsolutePath();
      log.info("Adding file : {}", absFilePath);
      playlist.add(file.getAbsolutePath());
    }
    log.info("Playlist added  {} songs.", playlist.size());
    walk.close();
    return playlist;
  }

  public List<String> getPlaylist(String name) {
    return playlists.get(name);
  }

  public Map<String, List<String>> getPlaylists() {
    return playlists;
  }

  public void startPlaylist() {
    startPlaylist(currentPlaylist, false, false, currentPlaylist);
  }

  public void startPlaylist(String playlist) {
    startPlaylist(playlist, false, false, playlist);
  }

  public void startPlaylist(String playlist, boolean shuffle, boolean repeat) {
    startPlaylist(playlist, shuffle, repeat, playlist);
  }

  public void startPlaylist(String playlist, boolean shuffle, boolean repeat, String track) {
    if (!playlists.containsKey(playlist)) {
      error("cannot play playlist %s does not exists", playlist);
      return;
    }
    playlistPlayer.start(playlists.get(playlist), shuffle, repeat, track);
  }

  public void stopPlaylist() {
    playlistPlayer.stop();
  }

  @Override
  public ServiceConfig getConfig() {

    AudioFileConfig c = (AudioFileConfig) config;
    // FIXME - remove members keep data in config !
    // FIXME - the following is not needed nor desired
    // useless self assignment
    c.mute = mute;
    c.currentTrack = currentTrack;
    c.currentPlaylist = currentPlaylist;
    c.volume = volume;
    c.playlists = playlists;
    c.peakMultiplier = peakMultiplier;
    // config.peakSampleInterval <- this one is done correctly no maintenance
    c.audioListeners = getAttached("publishAudio").toArray(new String[0]);

    return config;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    AudioFileConfig config = (AudioFileConfig) c;
    setMute(config.mute);
    setTrack(config.currentTrack);
    setVolume(config.volume);
    setPlaylist(config.currentPlaylist);
    if (config.playlists != null) {
      playlists = config.playlists;
    }

    if (config.audioListeners != null) {
      for (String listener : config.audioListeners) {
        attachAudioListener(listener);
      }
    }

    // FIXME - THIS IS ALL THATS NEEDED AND IT CAN BE
    // DONE IN THE SERVICE LEVEL
    // if services need "special" handling they can override
    this.config = c;
    return c;
  }

  public double publishPeak(float peak) {
    return peak;
  }

  public static void main(String[] args) {

    try {
      LoggingFactory.init("INFO");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      Runtime.start("python", "Python");

      AudioFile player = (AudioFile) Runtime.start("player", "AudioFile");
      player.play("https://upload.wikimedia.org/wikipedia/commons/1/1f/Bach_-_Brandenburg_Concerto.No.1_in_F_Major-_II._Adagio.ogg");

      boolean done = true;
      if (done) {
        return;
      }

      player.addListener("publishPeak", "servo", "moveTo");

      player.addPlaylist("acoustic", "/home/greg/Music/acoustic");
      player.addPlaylist("electronica", "/home/greg/Music/electronica");
      player.setPlaylist("electronica");
      player.startPlaylist("electronica");
      // audioPlayer.addPlaylist("my list", "Z:\\Music");

      // audioPlayer.playlist("my list" , true, false, "my list");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
