/**
 *
 * @author greg (at) myrobotlab.org
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
package org.myrobotlab.control;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.JTextField;

import javax.swing.SwingUtilities;

import org.myrobotlab.control.widget.MemoryWidget;
import org.myrobotlab.control.widget.NodeGUI;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import javax.swing.JTabbedPane;

import org.myrobotlab.memory.Node;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.service._TemplateService;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.interfaces.MemoryDisplay;
import org.slf4j.Logger;

public class GPSGUI extends ServiceGUI implements ActionListener {

    static final long serialVersionUID = 1L;
    public final static Logger log = LoggerFactory.getLogger(GPSGUI.class.getCanonicalName());
    private JTextField latitudeTextField = new JTextField(10);
    private JTextField longitudeTextField = new JTextField(10);
    private JTextField altitudeTextField = new JTextField(10);
    private JTextField stringTypeTextField = new JTextField(10);

    public GPSGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
        super(boundServiceName, myService, tabs);
    }

    public void init() {

        gc.gridheight = 8;
        gc.gridx = 0;
        gc.gridy = 0;


        display.add(new JLabel("String Type:"), gc);
        ++gc.gridx;
        display.add(stringTypeTextField, gc);
        gc.gridx = 0;
        gc.gridy += 42;


        display.add(new JLabel("Current Latitude:"), gc);
        ++gc.gridx;
        display.add(latitudeTextField, gc);
        gc.gridx = 0;
        gc.gridy += 42;


        display.add(new JLabel("Current Longitude:"), gc);
        ++gc.gridx;
        display.add(longitudeTextField, gc);
        gc.gridy += 42;
        gc.gridx = 0;


        display.add(new JLabel("Current Altitude:"), gc);
        ++gc.gridx;
        display.add(altitudeTextField, gc);
        gc.gridx = 0;

    }

    public void displatData (String[] tokens){
        stringTypeTextField.setText(tokens[0]);
        latitudeTextField.setText(tokens[2]);
        longitudeTextField.setText(tokens[4]);
        altitudeTextField.setText(tokens[9]);
    }
    
    
    public void getState(_TemplateService template) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            }
        });
    }

    @Override
    public void attachGUI() {
	subscribe("publishGGAData", "displatData", String[].class);       
    }

    @Override
    public void detachGUI() {
	unsubscribe("publishGGAData", "displatData", String[].class);    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
    }
}
