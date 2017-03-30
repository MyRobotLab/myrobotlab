package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.VideoWidget;
import org.slf4j.Logger;

public class VideoDisplayPanel {
  public class VideoMouseListener implements MouseListener {

    // FIXME addListener - relay to 
    @Override
    public void mouseClicked(MouseEvent e) {
      mouseInfo.setText(String.format("clicked %d,%d", e.getX(), e.getY()));
      Object[] params = new Object[2];
      params[0] = e.getX();
      params[1] = e.getY();

      // FIXME - to OpenCV specific ! - should just relay the event back - or send a more
      // generalized message or message specific to the Panel not to OpenCv
      myService.send(boundServiceName, "invokeFilterMethod", sourceNameLabel.getText(), "samplePoint", params);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // mouseInfo.setText("entered");
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // mouseInfo.setText("exit");

    }

    @Override
    public void mousePressed(MouseEvent e) {
      // mouseInfo.setText("pressed");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // mouseInfo.setText("release");
    }

  }

  public final static Logger log = LoggerFactory.getLogger(VideoDisplayPanel.class);
  VideoWidget parent;

  String boundFilterName;
  public final String boundServiceName;

  final SwingGui myService;
  public JPanel myDisplay = new JPanel();
  JLabel screen = new JLabel();
  JLabel mouseInfo = new JLabel("mouse x y");
  JLabel resolutionInfo = new JLabel("width x height");

  JLabel deltaTime = new JLabel("0");
  JLabel sourceNameLabel = new JLabel("");

  public JLabel extraDataLabel = new JLabel("");
  public SerializableImage lastImage = null;
  public ImageIcon lastIcon = new ImageIcon();
  public ImageIcon myIcon = new ImageIcon();

  public VideoMouseListener vml = new VideoMouseListener();

  public int lastImageWidth = 0;

  long displayModulus = 100;

  long delta = 0;

  public VideoDisplayPanel(String boundFilterName, VideoWidget p, SwingGui myService, String boundServiceName) {
    this(boundFilterName, p, myService, boundServiceName, null);
  }

  VideoDisplayPanel(String boundFilterName, VideoWidget parent, SwingGui myService, String boundServiceName, ImageIcon icon) {
    this.myService = myService;
    this.boundServiceName = boundServiceName;
    this.parent = parent;
    this.boundFilterName = boundFilterName;

    myDisplay.setLayout(new BorderLayout());

    if (icon == null) {
      screen.setIcon(Util.getResourceIcon("mrl_logo.jpg"));
    }

    screen.addMouseListener(vml);
    myIcon.setImageObserver(screen); // Good(necessary) Optimization

    myDisplay.add(BorderLayout.CENTER, screen);

    JPanel south = new JPanel();
    // add the sub-text components
    south.add(mouseInfo);
    south.add(resolutionInfo);
    south.add(deltaTime);
    south.add(sourceNameLabel);
    south.add(extraDataLabel);
    myDisplay.add(BorderLayout.SOUTH, south);

  }

  public void displayFrame(SerializableImage img) {

    /*
     * got new frame - check if a screen exists for it or if i'm in single
     * screen mode
     * 
     * img.source is the name of the bound filter
     */
    // ++frameCount;

    String source = img.getSource();

    if (lastImage != null) {
      screen.setIcon(lastIcon);
    }

    if (source != null && !sourceNameLabel.getText().equals(source)) {
      sourceNameLabel.setText(source);
    }

    if (parent.normalizedSize != null) {
      myIcon.setImage(img.getImage().getScaledInstance(parent.normalizedSize.width, parent.normalizedSize.height, 0));
    } else {
      BufferedImage bi = img.getImage();
      if (bi != null) {
        myIcon.setImage(bi);
      }
    }
    screen.setIcon(myIcon);

    if (img.frameIndex % displayModulus == 0 && lastImage != null) {
      delta = (img.getTimestamp() - lastImage.getTimestamp());
      if (delta != 0) {
        deltaTime.setText(String.format("%d fps", 1000 / delta));
      }
    }

    lastImage = img;
    lastIcon.setImage(img.getImage());

    if (lastImageWidth != img.getImage().getWidth()) {
      screen.invalidate();
      myService.pack();
      lastImageWidth = img.getImage().getWidth();
      resolutionInfo.setText(String.format("%dx%d", lastImageWidth, img.getImage().getHeight()));
    }

    img = null;

  }

} // VideoDisplayPanel
