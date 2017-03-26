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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.Twitter;
import org.slf4j.Logger;

public class TwitterGui extends ServiceGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(_TemplateServiceGui.class);

  JPasswordField consumerKey = new JPasswordField("XXXXXX", 20);
  JPasswordField consumerSecret = new JPasswordField("XXXXXX", 20);
  JPasswordField accessToken = new JPasswordField("XXXXXX", 20);
  JPasswordField accessTokenSecret = new JPasswordField("XXXXXX", 20);
  JButton setKeys = new JButton("set keys");
  JTextField text = new JTextField(20);
  JButton tweet = new JButton("tweet");
  Twitter twitter = null;

  public TwitterGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    display.setLayout(new BorderLayout());
    JPanel keyInfo = new JPanel(new GridLayout(0, 2));
    keyInfo.add(new JLabel("consumer key"));
    keyInfo.add(consumerKey);
    keyInfo.add(new JLabel("consumer secret"));
    keyInfo.add(consumerSecret);
    keyInfo.add(new JLabel("access token"));
    keyInfo.add(accessToken);
    keyInfo.add(new JLabel("access token secret"));
    keyInfo.add(accessTokenSecret);
    keyInfo.add(new JLabel(""));
    keyInfo.add(setKeys);
    setKeys.addActionListener(this);
    tweet.addActionListener(this);
    // display.setLayout(new BorderLayout());
    display.add(keyInfo, BorderLayout.NORTH);
    JPanel tweetPanel = new JPanel(new GridLayout(2, 1));
    tweetPanel.add(text);
    tweetPanel.add(tweet);
    display.add(tweetPanel, BorderLayout.SOUTH);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object o = event.getSource();
    if (o == setKeys) {
      myService.send(boundServiceName, "setSecurity", new String(consumerKey.getPassword()), new String(consumerSecret.getPassword()), new String(accessToken.getPassword()),
          new String(accessTokenSecret.getPassword()));
    } else if (o == tweet) {

      myService.send(boundServiceName, "tweet", new String(text.getText()));

    }

    // TODO Auto-generated method stub

  }

  public void onState(final Twitter twitter) {
    this.twitter = twitter;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        consumerKey.setText(twitter.consumerKey);
        consumerSecret.setText(twitter.consumerSecret);
        accessToken.setText(twitter.accessToken);
        accessTokenSecret.setText(twitter.accessTokenSecret);
        text.setText("Your Text Here");

      }
    });
  }
  
  public void setState() {
    myService.send(boundServiceName, "setState", twitter);
  }

}
