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

import edu.rice.cs.dynamicjava.symbol.JLSTypeSystem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.log4j.Logger;
import org.myrobotlab.service.SEAR;
import org.myrobotlab.service._TemplateService;
import org.myrobotlab.service.GUIService;
import java.io.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class SEARGUI extends ServiceGUI implements ActionListener {

    private File inputFile = null;
    private String filename;
    private String saveFilename;
    private JFileChooser saveProjectChooser;
    private JFileChooser projectChooser;
    private JTextField openTextField = new JTextField(32);
    private javax.swing.JButton openButton;
    private javax.swing.JButton openBrowseButton;
    private JTextField saveTextField = new JTextField(32);
    private javax.swing.JButton saveButton;
    private javax.swing.JButton saveBrowseButton;
    private javax.swing.JButton startButton;
    private JLabel status;
    static final long serialVersionUID = 1L;
    public final static Logger log = Logger.getLogger(SEARGUI.class.getCanonicalName());

    public SEARGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
        super(boundServiceName, myService, tabs);
    }

    public void init() {

        projectChooser = new javax.swing.JFileChooser();

        saveProjectChooser = new javax.swing.JFileChooser();  //Initialize file chooser for savefile

        gc.gridheight = 8;
        gc.gridx = 0;
        gc.gridy = 0;


        display.add(new JLabel("Open project"), gc);
        ++gc.gridx;
        display.add(openTextField, gc);
        //gc.gridy+=42;
        ++gc.gridy;


        gc.gridx += 150;
        openBrowseButton = new JButton("Browse...");
        display.add(openBrowseButton, gc);

        openBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openBrowseButtonActionPerformed(evt);
            }
        });





        gc.gridx += 150;
        openButton = new JButton("Open");
        display.add(openButton, gc);

        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });




        gc.gridx = 0;
        gc.gridy = 42;
        display.add(new JLabel("Save project"), gc);
        ++gc.gridx;
        display.add(saveTextField, gc);
        ++gc.gridy;

        gc.gridx += 150;
        saveBrowseButton = new JButton("Browse...");
        display.add(saveBrowseButton, gc);
        saveBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBrowseButtonActionPerformed(evt);
            }
        });



        gc.gridx += 150;
        saveButton = new JButton("Save");
//        openButton.addActionListener(del);
        display.add(saveButton, gc);


        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });



        gc.gridy += 84;
        gc.gridx = 0;
        startButton = new JButton("Start SEAR Simulation");
        display.add(startButton, gc);
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });



//        gc.gridy += 84;
        gc.gridx++;
        status = new JLabel("   Press 'Start' when you are ready to run the simulation");
        display.add(status, gc);


    }

//    DirectionEventListener del = new DirectionEventListener();
//
//    public class DirectionEventListener implements ActionListener {
//
//        @Override
//        public void actionPerformed(ActionEvent ae) {
//            log.info(ae);
//            if ("saveButton".equals(ae.getActionCommand())) {
//                System.out.println("Save hit");
//            }
//            else if ("openButton".equals(ae.getActionCommand())) {
//                System.out.println("open hit");
//            }
//           
//        }//end actionPerformed
//    }//end DirectionEventListener
    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {
        myService.send(boundServiceName, "loadProject", filename);

    }

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {
        myService.send(boundServiceName, "startSimulation");

    }

    private void openBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {

        int returnVal = projectChooser.showOpenDialog(getDisplay());   //initiate a fileChooser dialog
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            //Initialise file object
            inputFile = projectChooser.getSelectedFile();

            //read filename and store it in the text area
            filename = inputFile.getPath();

            openTextField.setText(filename);
        } else {
            //System.out.println("User Cancelled Action, or Error");
        }
        //filename = null; //reset the filename variable so another method can use it
    }

    public void getState(SEAR template) {
    }

    @Override
    public void attachGUI() {
        subscribe("getHashMap", "changeStatus", boolean.class);   
    }

    @Override
    public void detachGUI() {
        unsubscribe("getHashMap", "changeStatus", boolean.class);

    }

    public void changeStatus() {
        log.info("OK, I tried to change the status...");
        status.setText("   SEAR has loaded, You can run User Code now.");
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * ***
     * Taken from original SEAR and must be modified.
     */
    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {

        //Send message to SEAR.java to write the file to the given path
        myService.send(boundServiceName, "saveProject", saveFilename);
    }

    private void saveBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {


        int returnVal = saveProjectChooser.showSaveDialog(getDisplay());   //initiate a fileChooser dialog
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            //Initialize file object
            inputFile = saveProjectChooser.getSelectedFile();
            //read filename and store it in the text area
            saveFilename = inputFile.getPath();

            /*
             * The below block handles adding (or removing) the file extension
             */
            if (saveFilename.indexOf(".sear") < 0) //If the string does not contain ".zip"
            {
                saveFilename = saveFilename + ".sear";  //then add it to the end
            }
            //This was used when checking end of saveFileString, no longer needed, but here for posterity.
//           else{                // If the string has at least one .prj in it already
//String[] extensions = saveFilename.split(".");
//if (extensions.length>1)
//{
//    saveFilename=extensions[0];     //split it off and remove ".prj" from the string, the Filechooser will add it automatically.
//}
//} //end else string already had extension
            //Example saveProject file overwrite dialog form here: http://www.coderanch.com/t/346251/GUIService/java/deal-JFileChooser

            if (inputFile.exists()) { // IF the file already exists, ask to overwrite

                int answer = JOptionPane.showConfirmDialog(
                        saveProjectChooser, inputFile + " exists. Overwrite?", "Overwrite?",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer != JOptionPane.OK_OPTION) // If not OK to overwrite,
                {
                    return; //then return; close without writing.
                }
            }// end if exists


            saveTextField.setText(saveFilename);
            //  System.out.println("FILENAME is : "+ saveFilename);



        }// end approve saveProject file
        //otherwise write the file
        else {
//System.out.println("User Cancelled Action, or Error");
        }
        // saveFilename = null; //reset the filename variable so another method can use it
        //inputFile = null;
        // System.out.println("======================PROJECT SAVED ====================");

    }// end saveFile
}