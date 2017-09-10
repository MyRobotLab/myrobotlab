/**
 *
 * @author kmcgerald (at) myrobotlab.org
 *
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version (subject to the "Classpath" exception as provided in the LICENSE.txt
 * file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for details.
 *
 * Enjoy !
 *
 *
 */
package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Mqtt;
import org.myrobotlab.service.Mqtt.MqttMsg;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

public class MqttGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MqttGui.class);
  
  // meta data/config
  JTextField url = new JTextField(15);
  
  JComboBox<String> subscriptions = new JComboBox<String>();
  
  // input
  JTextArea recvData = new JTextArea(5, 20);
  JScrollPane onMsg = new JScrollPane(recvData);
  
  JTextArea sendData = new JTextArea(5, 20);
  JScrollPane sendMsg = new JScrollPane(sendData);
  
  JTextField topic = new JTextField(30);
  JButton send = new JButton("send");
  JButton subscribe = new JButton("subscribe");
  
  int qos = 2;


  public MqttGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    
    recvData.setEditable(false);
    DefaultCaret caret = (DefaultCaret)recvData.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    
    addTopLine(" url ", url);
    addTopLine(" subscriptions ", subscriptions);
    addTopLine(" messages", "");
    /*
    JPanel sendToTopicPanel = new JPanel();
    sendToTopicPanel.add(send);
    sendToTopicPanel.add(topic);
    sendToTopicPanel.add(subscribe);
    */
    addLine(onMsg);
    addLine(sendMsg);
    addBottomLine(createFlowPanel("topic", send, topic, subscribe));
    
    send.addActionListener(this);
    subscribe.addActionListener(this);
    // addTopLine("time", timeTextField);
    // addTopLine("topic", topicTextField, subscribe);
    // sendMsg, send
    // addTopLine("messages", onMsg);
    // addLine(onMsg);
  }
  
  public void onMqttMsg(MqttMsg msg){
    recvData.append(String.format("%s: %s %s\n", new Timestamp(System.currentTimeMillis()).toString(), msg.topic, new String(msg.payload)));
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    if (o == send){
      send("publish", topic.getText(), qos, sendData.getText().getBytes());
    }
    if (o == subscribe){
      send("subscribe", topic.getText());
    }
  }

  @Override
  public void subscribeGui() {
    subscribe("publishMqttMsg");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishMqttMsg");
  }
 
  public void onState(final Mqtt mqtt) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        url.setText(mqtt.getUrl());
        // subscribeToTopic.setText(mqtt.getLastTopic());
        Set<String> subs = mqtt.getSubscriptions();
        qos = mqtt.getQos();
        subscriptions.removeAllItems();
        for (String s : subs){
          subscriptions.addItem(s);
        }
        topic.setText(mqtt.getTopic());
        subscriptions.setSelectedItem(mqtt.getTopic());
        // fixme JList of subscriptions ...
        
      }
    });
  }

}
