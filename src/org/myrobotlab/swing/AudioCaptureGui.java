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

package org.myrobotlab.swing;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.myrobotlab.service.AudioCapture;
import org.myrobotlab.service.SwingGui;

public class AudioCaptureGui extends ServiceGui {

  static final long serialVersionUID = 1L;

  final JButton captureBtn = new JButton("Capture");
  final JButton stopBtn = new JButton("Stop");
  final JButton playBtn = new JButton("Playback");

  public AudioCaptureGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    // Register anonymous listeners
    captureBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        captureBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        playBtn.setEnabled(false);
        // Capture input data from the
        // microphone until the Stop
        // button is clicked.
        myService.send(boundServiceName, "captureAudio");
      }// end actionPerformed
    }// end ActionListener
    );// end addActionListener()
    display.add(captureBtn);

    stopBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        captureBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        playBtn.setEnabled(true);
        // Terminate the capturing of
        // input data from the
        // microphone.
        myService.send(boundServiceName, "stopAudioCapture");
      }// end actionPerformed
    }// end ActionListener
    );// end addActionListener()
    display.add(stopBtn);

    playBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // Play back all of the data
        // that was saved during
        // capture.
        myService.send(boundServiceName, "playAudio");
      }// end actionPerformed
    }// end ActionListener
    );// end addActionListener()
    display.add(playBtn);

    display.setLayout(new FlowLayout());

  
  }
  
  public void onState(final AudioCapture audiocapture){
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        if (AudioCapture.stopCapture) {
          captureBtn.setEnabled(true);
          stopBtn.setEnabled(false);
        } else {
          captureBtn.setEnabled(false);
          stopBtn.setEnabled(true);
        }
      }
    });
  }

  @Override
  public void subscribeGui() {
  }

  @Override
  public void unsubscribeGui() {
  }


}
