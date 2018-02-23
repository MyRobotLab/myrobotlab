/**
 * InMoov V2 White page - The InMoov Service ( refactor WIP ).
 * 
 * The InMoov service allows control of the InMoov robot. This robot was created
 * by Gael Langevin. It's an open source 3D printable robot. All of the parts
 * and instructions to build are on http://www.inmoov.fr/). InMoov is a composite of servos, Arduinos,
 * microphone, camera, kinect and computer. The InMoov service is composed of
 * many other services, and allows easy initialization and control of these
 * sub systems.
 *
 */
package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service._TemplateService;
import org.slf4j.Logger;
import javax.swing.JTabbedPane;

public class InMoovV2Gui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoovV2Gui.class);
  private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

  public InMoovV2Gui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    setTitle("InMoov V2");

  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    // TODO Auto-generated method stub
  }


  public void onState(_TemplateService template) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
      }
    });
  }
}
