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
import org.myrobotlab.service.interfaces.Point2dListener;
import org.myrobotlab.swing.VideoWidget2;
import org.slf4j.Logger;

public class VideoDisplayPanel2 {
  public class VideoMouseListener implements MouseListener {

    Point2dListener listener = null;

    @Override
    public void mouseClicked(MouseEvent e) {
      mouseInfo.setText(String.format("clicked %d,%d", e.getX(), e.getY()));

      if (listener != null) {
        listener.onSamplePoint(sourceNameLabel.getText(), e.getX(), e.getY());
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
  }

  public final static Logger log = LoggerFactory.getLogger(VideoDisplayPanel2.class);
  VideoWidget2 parent;

  String boundFilterName;
  public final String boundServiceName;

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

  public VideoDisplayPanel2(String boundFilterName, VideoWidget2 parent, String boundServiceName, ImageIcon icon) {
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

    BufferedImage image = img.getImage();
    String source = img.getSource();

    if (image == null) {
      return;
    }

    if (lastImage != null) {
      screen.setIcon(lastIcon);
    }

    if (source != null && !sourceNameLabel.getText().equals(source)) {
      sourceNameLabel.setText(source);
    }

    BufferedImage bi = img.getImage();
    if (bi != null) {
      myIcon.setImage(bi);
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
      parent.pack();
      lastImageWidth = img.getImage().getWidth();
      resolutionInfo.setText(String.format("%dx%d", lastImageWidth, img.getImage().getHeight()));
    }

    img = null;

  }

} // VideoDisplayPanel
