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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.net.Connection;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.ConnectionNodeList;

public class RemoteAdapterGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  JLabel numClients = new JLabel("0");

  JButton scan = new JButton("scan");
  JButton connect = new JButton("connect");
  JButton listen = new JButton("listen");
  JLabel udpPort = new JLabel("");
  JLabel tcpPort = new JLabel("");
  JLabel newConn = new JLabel("");

  // display of the Connections
  ConnectionNodeList list = new ConnectionNodeList();
  String lastProtoKey;

  RemoteAdapter myRemote = null;

  public RemoteAdapterGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    display.setLayout(new BorderLayout());

    JPanel top = new JPanel();
    top.add(scan);
    top.add(connect);
    top.add(listen);
    top.add(new JLabel("udp "));
    top.add(udpPort);
    top.add(new JLabel("tcp "));
    top.add(tcpPort);

    scan.addActionListener(this);
    connect.addActionListener(this);
    listen.addActionListener(this);

    display.add(top, BorderLayout.NORTH);
    display.add(list, BorderLayout.CENTER);
  }

  @Override
  public void actionPerformed(ActionEvent action) {
    Object o = action.getSource();
    if (o == connect) {
      String newProtoKey = (String) JOptionPane.showInputDialog(myService.getFrame(), "<html>connect to a remote MyRobotLab</html>", "connect", JOptionPane.WARNING_MESSAGE,
          Util.getResourceIcon("RemoteAdapter/connect.png"), null, lastProtoKey);

      if (newProtoKey == null || newProtoKey == "") {
        return;
      }

      send("connect", newProtoKey);
      lastProtoKey = newProtoKey;
    } else if (o == listen) {
      if (listen.getText().equals("stop listening")) {
        send("stopListening");
      } else {
        send("startListening");
      }
    } else if (o == scan) {
      if (scan.getText().equals("stop scanning")) {
        send("stopScanning");
        send("broadcastState");
      } else {
        send("scan");
        send("broadcastState");
      }
    }
  }

  @Override
  public void subscribeGui() {
    subscribe("publishConnect");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishConnection");
  }

  public void onState(final RemoteAdapter remote) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        myRemote = remote;
        if (myRemote.isScanning()) {
          scan.setText("stop scanning");
        } else {
          scan.setText("scan");
        }
        if (myRemote.isListening()) {
          listen.setText("stop listening");
          if (remote.getUdpPort() != null) {
            udpPort.setText(remote.getUdpPort().toString());
          } else {
            udpPort.setText("");
          }
          if (remote.getTcpPort() != null) {
            tcpPort.setText(remote.getTcpPort().toString());
          } else {
            tcpPort.setText("");
          }
        } else {
          listen.setText("listen");
        }
        lastProtoKey = remote.lastProtocolKey;
        if (remote.getClients() == null) {
          return;
        }
        list.model.clear();
        for (Map.Entry<URI, Connection> o : remote.getClients().entrySet()) {
          // URI uri = o.getKey();
          Connection data = o.getValue();
          list.model.add(0, data);
        }
      }
    });
  }


  public void onNewConnection(Connection conn) {
    myService.info("new connection found %d %s", System.currentTimeMillis(), conn.toString());
  }

}
