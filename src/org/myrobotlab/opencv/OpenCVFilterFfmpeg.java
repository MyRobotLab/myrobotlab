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
 * http://docs.opencv.org/modules/imgproc/doc/feature_detection.html
 * http://stackoverflow.com/questions/19270458/cvcalcopticalflowpyrlk-not-working-as-expected
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class OpenCVFilterFfmpeg extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFfmpeg.class);
  FFmpegFrameRecorder z = null;
  FFmpegFrameGrabber x = null;
  FFmpegFrameRecorder recorder = null;
  boolean recording = true;
  int sampleAudioRateInHz = 44100;
  int frameRate = 30;

  transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

  public OpenCVFilterFfmpeg() {
    super();
  }

  public OpenCVFilterFfmpeg(String name) {
    super(name);
  }

  @Override
  public IplImage display(IplImage frame, OpenCVData data) {

    return frame;
  }

  @Override
  public void imageChanged(IplImage image) {
    initRecorder(String.format("%s.%d.mp4", name, System.currentTimeMillis()));
  }

  void initRecorder(String filename) {

    try {
      log.info(String.format("initRecorder %s", filename));

      // filename = "rtp"
      // filename = "rtmp://0.0.0.0:1888/video/test.flv";

      // filename = "rtmp://live:live@128.122.151.108:1935/live/test.flv";

      filename = "rtmp://live:live@54.158.155.69:1935/live/test.flv";
      filename = "test2.mp4";
      filename = "rtmp://127.0.0.1:1935/test.flv";
      filename = "test2.mp4.h264.mp4";
      filename = "rtmp://demo.myrobotlab.org:8090/feed1.ffm";
      filename = "http://demo.myrobotlab.org:8090/feed1.ffm";

      recorder = new FFmpegFrameRecorder(filename, imageSize.width(), imageSize.height(), 0);

      // recorder.setFormat("flv");
      // recorder.setFormat("mp4");
      // recorder.setFormat("3gp"); // h263
      // recorder.setFormat("avi");
      // recorder.setVideoOption("preset", "ultrafast");
      // recorder.setSampleRate(sampleAudioRateInHz);
      // recorder.setImageWidth(imageSize.width());
      // recorder.setImageHeight(imageSize.height());
      // recorder.
      // avcodec.AV_CODEC_ID_FLV1;
      // recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
      // recorder.s

      // re-set in the surface changed method as well
      // recorder.setFrameRate(frameRate);

      // recorder.setVideoBitrate(16384);
      // recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

      recorder.setFormat("flv");
      recorder.setSampleRate(sampleAudioRateInHz);
      recorder.setFrameRate(30);
      recorder.setVideoBitrate(30 * 640 * 480);
      recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
      recorder.setVideoOption("preset", "ultrafast");

      /*
       * 
       * recorder = new FFmpegFrameRecorder(rtmplink, 320, 568, 2);
       * recorder.setInterleaved(true);
       * recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
       * recorder.setVideoBitrate(videoBitRate);
       * recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
       * recorder.setFormat("flv"); recorder.setVideoOption("preset",
       * "veryfast"); recorder.setVideoOption("tune", "zerolatency");
       * recorder.setGopSize(GOP_LENGTH_IN_FRAMES);
       * recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
       * recorder.setSampleRate(sampleAudioRateInHz);
       * recorder.setAudioBitrate(audioBitRate);
       * recorder.setFrameRate(frameRate);
       */

      recorder.start();

      log.info("recorder.setFrameRate(frameRate)");

      // Create audio recording thread
      // audioRecordRunnable = new AudioRecordRunnable();
      // audioThread = new Thread(audioRecordRunnable);

      /*
       * 
       * recorder = new FFmpegFrameRecorder(filePath, width, height);
       * recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
       * recorder.setFormat("mp4"); recorder.setFrameRate(VIDEO_FPS);
       * recorder.setVideoBitrate(16384);
       * recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
       */

      /*
       * FrameRecorder recorder = new FFmpegFrameRecorder(
       * 'newvideo.mp4",grabber.getImageWidth(),grabber.getImageHeight());
       * recorder.setFormat("mp4");
       * recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
       * recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
       * recorder.setFrameRate(5); int height=grabber.getImageHeight(); int
       * width=grabber.getImageWidth(); int bitrate=(int) (height*width*5*0.07);
       * recorder.setVideoBitrate(bitrate); System.out.println(
       * "Grabber videoprocess Bitrate::"+bitrate);
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  // Start the capture
  public void startRecording() {
    try {
      if (recorder == null) {
        initRecorder(name);
      }
      recorder.start();
      // startTime = System.currentTimeMillis();
      recording = true;
      // audioThread.start();
    } catch (FFmpegFrameRecorder.Exception e) {
      e.printStackTrace();
    }
  }

  public void stopRecording() {
    // This should stop the audio thread from running
    // runAudioThread = false;

    if (recorder != null && recording) {
      recording = false;
      log.info("Finishing recording, calling stop and release on recorder");
      try {
        recorder.stop();
        recorder.release();
      } catch (Exception e) {
        Logging.logError(e);
      }
      recorder = null;
    }
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {

    // TODO: support audio?
    // boolean runAudioThread = true;

    // Set the thread priority
    // android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

    // Audio
    // int bufferSize = 2048;
    // short[] audioData;
    // int bufferReadResult;

    // bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
    // AudioFormat.CHANNEL_CONFIGURATION_MONO,
    // AudioFormat.ENCODING_PCM_16BIT);
    // audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
    // sampleAudioRateInHz, AudioFormat.CHANNEL_CONFIGURATION_MONO,
    // AudioFormat.ENCODING_PCM_16BIT, bufferSize);

    // audioData = new short[bufferSize];

    log.info("audioRecord.startRecording()");
    // audioRecord.startRecording();

    // Audio Capture/Encoding Loop
    // while (runAudioThread) {
    // Read from audioRecord
    // bufferReadResult = audioRecord.read(audioData, 0,
    // audioData.length);
    // if (bufferReadResult > 0) {
    // log.info("audioRecord bufferReadResult: " + bufferReadResult);

    // Changes in this variable may not be picked up despite it being
    // "volatile"
    if (recording) {
      try {

        // recorder.setTimestamp(videoTimestamp);

        recorder.record(converter.convert(image));

        /*
         * Buffer[] buffer = {ShortBuffer.wrap(audioData, 0, bufferReadResult)};
         * recorder.record(buffer);
         * 
         * // Write to FFmpegFrameRecorder
         * recorder.record(ShortBuffer.wrap(audioData, 0, bufferReadResult));
         */

        // recorder.stop();

      } catch (FFmpegFrameRecorder.Exception e) {
        log.info(e.getMessage());
        e.printStackTrace();
      }
    }

    // }
    log.info("AudioThread Finished");

    /* Capture/Encoding finished, release recorder */
    /*
     * if (audioRecord != null) { audioRecord.stop(); audioRecord.release();
     * audioRecord = null; log.info("audioRecord released"); }
     */
    return image;
  }

  public void release() {
    stopRecording();
  }

}
