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
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.AndroidSpeechRecognition;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.WebkitSpeechRecognition;
import org.myrobotlab.service.WebGui;
import org.slf4j.Logger;

public class AndroidSpeechRecognitionGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(_TemplateServiceGui.class);

  private JTextField onRecognized = new JTextField("Waiting orders...", 24);
  BufferedImage microOn = ImageIO.read(FileIO.class.getResource("/resource/InMoov/monitor/microOn.png"));
  BufferedImage microOff = ImageIO.read(FileIO.class.getResource("/resource/InMoov/monitor/microOff.png"));

  private JButton micro = new JButton(new ImageIcon(microOn));

  private JButton autoListen = new JButton("Auto Listening ON");
  private JButton startServer = new JButton("Start Server");
  private boolean listeningStatus = false;
  private JLabel serverStatus = new JLabel("- Server : not listening");
  private JLabel clientStatus = new JLabel("- Client : not connected");
  
  public AndroidSpeechRecognitionGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);

    display.setLayout(new BorderLayout());
    
    JPanel panNetwork = new JPanel();
    panNetwork.setLayout(new BoxLayout(panNetwork, BoxLayout.Y_AXIS));
    panNetwork.setBorder(BorderFactory.createTitledBorder("Network"));
    panNetwork.add(serverStatus);
    panNetwork.add(clientStatus);
    
    JPanel pan1 = new JPanel();
    pan1.setBorder(BorderFactory.createTitledBorder("Control"));

    pan1.add(micro);
    pan1.add("onRecognized : ", onRecognized);
    autoListen.addActionListener(this);
    autoListen.setBackground(Color.RED);
    startServer.addActionListener(this);
    startServer.setBackground(Color.RED);
    micro.addActionListener(this);

    JPanel pan2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    pan2.add(startServer);
    pan2.add(autoListen);
    display.add(panNetwork, BorderLayout.PAGE_START);
    display.add(pan1, BorderLayout.CENTER);
    display.add(pan2, BorderLayout.PAGE_END);

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

        if (o == startServer) {
          if (startServer.getText().equals("Start Server")) {
            send("startServer");
          } else {
            send("stopServer");
          }
          return;
        }


        if (o == micro) {
          if (listeningStatus) {
            send("pauseListening");
          } else {
            send("resumeListening");
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
  public void onState(final AndroidSpeechRecognition AndroidSpeechRecognition) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        onRecognized.setText(AndroidSpeechRecognition.lastThingRecognized);        
        clientStatus.setText(AndroidSpeechRecognition.getClientAddress());
       
        if (AndroidSpeechRecognition.getAutoListen()) {
          autoListen.setText("Auto Listening ON");
          autoListen.setBackground(Color.green);
        } else {
          autoListen.setText("Auto Listening OFF");
          autoListen.setBackground(Color.RED);
        }
        
        if (AndroidSpeechRecognition.runningserver) {
          startServer.setText("Stop Server");
          startServer.setBackground(Color.green);
          serverStatus.setText("- Server : Listening > "+AndroidSpeechRecognition.getServerAddress()+":"+AndroidSpeechRecognition.port);
          
        } else {
          startServer.setText("Start Server");
          startServer.setBackground(Color.RED);
          serverStatus.setText("- Server : not listening");
        }

        if (AndroidSpeechRecognition.isListening()) {
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
