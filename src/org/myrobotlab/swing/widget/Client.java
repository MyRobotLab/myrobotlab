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

package org.myrobotlab.swing.widget;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import javax.swing.JApplet;
import javax.swing.JPanel;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class Client extends JApplet {

  public final static Logger log = LoggerFactory.getLogger(Client.class);

  private static final long serialVersionUID = 1L;
  private JPanel jContentPane = null;

  private String appletHostAddress = null;
  private InetAddress appletAddress = null;
  private String codeBaseHostAddress = null;

  private SwingGui guiService;

  public Client() {
    super();
  }

  private JPanel getJContentPane() {
    if (jContentPane == null) {
      jContentPane = new JPanel();
      jContentPane.add(guiService.getDisplay());
    }
    return jContentPane;
  }

  @Override
  public void init() {
    this.setSize(500, 600);
    try {
      LoggingFactory.init(Level.WARN);
      // determine network details - can only accurately determine applet
      // IP from server request
      codeBaseHostAddress = getCodeBase().getHost();
      appletAddress = InetAddress.getLocalHost();
      appletHostAddress = appletAddress.getHostAddress();

      /*
       * ConfigurationManager cfg = new ConfigurationManager(null);
       * cfg.set("hostname", appletAddress.getHostName());
       * cfg.setRoot(appletAddress.getHostName()); cfg.set("servicePort", 0); //
       * Applet - can not have a service port
       */
      // TODO - hardcode set SwingGui to display only appropriate
      // components !!!
      guiService = new SwingGui(appletAddress.getHostName() + " gui");
      guiService.startService();

      if (codeBaseHostAddress.length() == 0) {
        codeBaseHostAddress = "localhost"; // for faking out the applet
        // wrapper when running in
        // the ide
      }

      log.info("appletAddress [" + appletAddress + "]");
      log.info("appletHostAddress [" + appletHostAddress + "]");
      log.info("codeBaseHostAddress [" + codeBaseHostAddress + "]");
      log.info("getCodeBase [" + getCodeBase() + "]");

      // Multicast Begin
      // --------------------------------------------------
      // TODO - TCP/IP control assigns MultiCast address:port & datatype +
      // control messages stop/start switch
      MulticastSocket server = new MulticastSocket(1234);
      InetAddress group = InetAddress.getByName("234.5.6.7");
      server.joinGroup(group);
      boolean infinite = true;

      /* Continually receives data and prints them */
      while (infinite) {
        byte buf[] = new byte[1024];
        DatagramPacket data = new DatagramPacket(buf, buf.length);
        server.receive(data);
        String msg = new String(data.getData()).trim();
        System.out.println(msg);
      }
      server.close();
      // Multicast End --------------------------------------------------

    } catch (UnknownHostException e) {
      log.error("Couldn't get Internet appletAddress: Unknown appletHostAddress");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      Logging.logError(e);
    }

    this.setContentPane(getJContentPane());

  }

}
