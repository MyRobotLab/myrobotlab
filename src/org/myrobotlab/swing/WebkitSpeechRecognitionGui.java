/**
 *                    
WebkitSpeechRecognition GUI - WIP
 * 
 * */

package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.WebkitSpeechRecognition;
import org.slf4j.Logger;

public class WebkitSpeechRecognitionGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(_TemplateServiceGui.class);
  
  JLabel onRecognized = new JLabel("Waiting orders...");


  public WebkitSpeechRecognitionGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    
    setTitle("WebkitSpeechRecognition control");

    addLine("onRecognized : ", onRecognized);
   
 
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {

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

  /**
   * Service State change - this method will be called when a "broadcastState"
   * method is called which triggers a publishState.  This event handler is typically
   * used when data or state information in the service has changed, and the UI should
   * update to reflect this changed state.
   * @param template
   */
  public void onState(WebkitSpeechRecognition WebkitSpeechRecognition) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
    	  onRecognized.setText(WebkitSpeechRecognition.lastThingRecognized);
      }
    });
  }

}
