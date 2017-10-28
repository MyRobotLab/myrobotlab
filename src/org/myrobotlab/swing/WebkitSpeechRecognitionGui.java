/**
 *                    
WebkitSpeechRecognition GUI - WIP
 * 
 * */

package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.WebkitSpeechRecognition;
import org.myrobotlab.service.WebGui;
import org.slf4j.Logger;

public class WebkitSpeechRecognitionGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(_TemplateServiceGui.class);

  private JTextField onRecognized = new JTextField("Waiting orders...", 24);
  BufferedImage microOn = ImageIO.read(FileIO.class.getResource("/resource/InMoov/monitor/microOn.png"));
  BufferedImage microOff = ImageIO.read(FileIO.class.getResource("/resource/InMoov/monitor/microOff.png"));

  private JButton micro = new JButton(new ImageIcon(microOn));

  private JButton startWebGui = new JButton("Start WebGui");
  private JButton autoListen = new JButton("Auto Listening ON");
  private JButton continuous = new JButton("Speedup recognition OFF");
  private boolean listeningStatus = false;

  public WebkitSpeechRecognitionGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);

    display.setLayout(new BorderLayout());
    JPanel pan1 = new JPanel();
    pan1.setBorder(BorderFactory.createTitledBorder("Status"));

    pan1.add(micro);
    pan1.add("onRecognized : ", onRecognized);
    autoListen.addActionListener(this);
    autoListen.setBackground(Color.RED);
    continuous.addActionListener(this);
    continuous.setBackground(Color.RED);
    startWebGui.addActionListener(this);
    micro.addActionListener(this);

    JPanel pan2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    pan2.add(startWebGui);
    pan2.add(autoListen);
    pan2.add(continuous);

    display.add(pan1, BorderLayout.PAGE_START);
    display.add(pan2, BorderLayout.CENTER);

    // pan.add("onRecognized : ", onRecognized);

  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Object o = event.getSource();

        if (o == autoListen) {
          if (autoListen.getText().equals("Auto Listening ON")) {
            send("setAutoListen", false);
          } else {
            send("setAutoListen", true);
          }
          return;
        }

        if (o == continuous) {
          if (continuous.getText().equals("Speedup recognition OFF")) {
            send("setContinuous", false);
          } else {
            send("setContinuous", true);
          }
          return;
        }

        if (o == startWebGui) {
          WebGui webgui = (WebGui) Runtime.create("WebGui", "WebGui");
          if (!webgui.isStarted()) {
            webgui.autoStartBrowser(false);
            webgui.startService();
            webgui.startBrowser("http://localhost:8888/#/service/" + boundServiceName);
          }
          startWebGui.setText("WebGui is started");
          return;
        }

        if (o == micro) {
          if (listeningStatus) {
            send("onStartSpeaking", "");
          } else {
            send("startListening");
          }
          return;
        }
      }
    });
  }

  @Override
  public void subscribeGui() {
    // un-defined gui's

    // subscribe("someMethod");
    // send("someMethod");
  }

  @Override
  public void unsubscribeGui() {
    // commented out subscription due to this class being used for
    // un-defined gui's

    // unsubscribe("someMethod");
  }

  /*
   * Service State change - this method will be called when a "broadcastState"
   * method is called which triggers a publishState. This event handler is
   * typically used when data or state information in the service has changed,
   * and the UI should update to reflect this changed state.
   */
  public void onState(final WebkitSpeechRecognition WebkitSpeechRecognition) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        onRecognized.setText(WebkitSpeechRecognition.lastThingRecognized);
        if (WebkitSpeechRecognition.getautoListen()) {
          autoListen.setText("Auto Listening ON");
          autoListen.setBackground(Color.green);
        } else {
          autoListen.setText("Auto Listening OFF");
          autoListen.setBackground(Color.RED);
        }

        if (WebkitSpeechRecognition.getContinuous()) {
          continuous.setText("Speedup recognition OFF");
          continuous.setBackground(Color.RED);
        } else {
          continuous.setText("Speedup recognition ON");
          continuous.setBackground(Color.GREEN);
        }

        if (WebkitSpeechRecognition.isListening()) {
          micro.setIcon(new ImageIcon(microOn));
          listeningStatus = true;
        } else {
          micro.setIcon(new ImageIcon(microOff));
          listeningStatus = false;
        }

      }
    });
  }

}
