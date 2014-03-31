package org.myrobotlab.service;

import org.SEAR.SensorSandboxALL;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.SEAR.GPSSimulator;
import org.SEAR.LIDARSimulator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.reflection.Instantiator;

public class SEAR extends Service {

    private static final long serialVersionUID = 1L;
    public static SensorSandboxALL searSim = new SensorSandboxALL();
    public final static Logger log = Logger.getLogger(SEAR.class.getCanonicalName());
    public String openPath = "";
    public String savePath = "";
    ArrayList<String> fileList;
    HashMap< String, Object> serialDevices;

    public SEAR(String n) {
        super(n, SEAR.class.getCanonicalName());
    }

//    @Override
    public void loadDefaultConfiguration() {
    }

    @Override
    public String getDescription() {
        return "used as a general template";
    }

    public static void main(String[] args) throws InterruptedException {
        org.apache.log4j.BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        SEAR template = new SEAR("SEAR");
        template.startService();


        //Start a Python service
        Python python = new Python("python");
        python.startService();

        //Start the GUIService service
        GUIService gui = new GUIService("gui");
        gui.startService();
        

    }

//    @Override 
//public void stopService()
//{
////    searSim.rootNode.detachAllChildren();
//    searSim.stop(true);
////    System.exit(0);
//super.stopService();
///*
//* add any additional clean-up here
//*/
//}
    public float getDistanceResult(String sensorNum) {
        return searSim.getDistanceResult(sensorNum);
    }

    public float getOdomResult() {
        return searSim.getOdomResult();
    }

    public void drive(float speed, float radius) {
        searSim.drive(speed, radius);
    }

    public void stopSimulation() {
        searSim.stop();
    }

//    @Override
//    public void stopService() {
//        //unsubscribe from everything
//        for (String key : serialDevices.keySet()) {
//            log.info("Key = " + key);
////        using this method unsubscribe(String publisherName, String outMethod, String inMethod)
//            unsubscribe(key, "publishByte", "routeBytes");
//        }
//        super.stopService();
//    }

    public void startSimulation(boolean debugShapes) throws InterruptedException {

        AppSettings simWindowSettings = new AppSettings(true);
        simWindowSettings.setTitle("Simulation Environment for Autonomous Robots");
        searSim.setSettings(simWindowSettings);

        searSim.setShowSettings(false);  //Prevents the Jmonkey dialog window from popping up. Must be set before app.start()
        searSim.setPauseOnLostFocus(false); //keeps simulation going while window is not in focus or selected. Allows it to run in the background.

        if (debugShapes) {
            searSim.setDebugMode();
        }

        searSim.startSimulation(this.getName());

        while (!searSim.initialized()) {
            Thread.sleep(333); //wait 1/3 of a second and check  
        }
        invoke("getHashMap");
    }

    public void getHashMap() {
        serialDevices = searSim.serialDeviceRegistry;
        log.info("You should be able to start your usercode now...");

        //iterating over keys only eg. "myLidar_LIDARsimulator_Serial_Service"
        for (String key : serialDevices.keySet()) {
            log.info("Key = " + key);
        }
    }//end getHashMap

    public void routeBytes(int[] bytes) {
        log.warn("SEARService is routing bytes to appropriate simulator service");
// here you'd look the at the hashmap and then do something 
    }

    @Override
    public boolean preProcessHook(Message m) {

        // and dialog methods ?!?!?
        // and dialog methods ?!?!?

        // if the method name is == to a method in the SEARService
        if (methodSet.contains(m.method)) {
            // process the message like a regular service
            return true;
        }

        // otherwise send the message to the dialog with the senders name



        /*
         * Get the instance of the class that SEARservice needs to send the 
         * serial messages to
         */



        /* 
         * Find what service it should go to by finding out what kind of 
         * service it is, then strip everything but the parent service's name
         * for instance "myLidar_LIDARsimulator_Serial_Service" becomes "myLidar"
         */
        Object simulator = serialDevices.get(m.sender);

        String instanceName;
        if (m.sender.contains("_LIDARsimulator")) {
            instanceName = m.sender.substring(0, m.sender.lastIndexOf("_LIDARsimulator_Serial_Service"));
            simulator = (LIDARSimulator) simulator;
        } else if (m.sender.contains("_GPSsimulator")) // otherwise send the message to the dialog with the senders name
        {
            simulator = (GPSSimulator) simulator;
            instanceName = m.sender.substring(0, m.sender.lastIndexOf("_GPSsimulator_Serial_Service"));
        } else {
            error("serialDevice instanceName not found");
            return false;
        }

        try {
            Method method = simulator.getClass().getMethod(m.method);
            log.info(String.format("my simulator type is %s", simulator.getClass().getSimpleName()));
        } catch (NoSuchMethodException | SecurityException e) {
            error("Something did not jive while attempting to send the serial data from SEARservice to " + m.sender);
            Logging.logException(e);
        }

//        Instantiator.invokeMethod(simulator, m.method, m.data);
        return false;
    } //end preProcessorHook

//       public int[] sendMessage(String lidarName, String message) {
//        return searSim.toggleLidarView(lidarName);
//    }    
    public void relayLidarData() {
        System.out.println("inside relay LIDAR data");
    }

    public void loadProject(String filePath) {
        System.out.println("Open " + filePath);

        this.openPath = filePath;
        unZipIt(filePath);

        //Do what you need to open the project to this path
    }

    /**
     * Blindly saves all files associated with this particular SEARProject to
     * the zip file location selected by the user. Reference: Zipping
     * http://www.mkyong.com/java/how-to-compress-files-in-zip-format/ Reference
     * unZipping
     * http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/
     *
     * @param filename
     */
    public void saveProject(String saveFilename) {

        fileList = new ArrayList<String>();
//        System.out.println("saveAll called with "+saveFilename);

        // List all the files in the Robot directory
        String sourceDir = String.format("%1$s%2$sSEARproject", System.getProperty("user.dir"), File.separator);
//        System.out.println("Source directory :"+sourceDir);

//        generate file list of this directory
        generateFileList(new File(sourceDir));

        zipIt(saveFilename, sourceDir);

    }

    /**
     * Zip a SEAR project Reference;
     * http://www.mkyong.com/java/how-to-compress-files-in-zip-format/
     *
     * @param zipFile output ZIP file location
     */
    public void zipIt(String zipFile, String sourceFolder) {


        byte[] buffer = new byte[1024];

        try {

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

//    	System.out.println("Output to Zip : " + zipFile);

            for (String file : fileList) {

//              System.out.println("Relative Path = "+getRelativePath(sourceFolder));

//    		System.out.println("File Added : " + file);
                ZipEntry ze = new ZipEntry(getRelativePath(file));

                zos.putNextEntry(ze);

                FileInputStream in =
                        new FileInputStream(file);

                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                in.close();
            }

            zos.closeEntry();
            //remember close it
            zos.close();

//    	System.out.println("Done");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getRelativePath(String fileName) {

        String base = String.format("%1$s%2$s", System.getProperty("user.dir"), File.separator);

        String relative = new File(base).toURI().relativize(new File(fileName).toURI()).getPath();
        return relative;
    }

    /**
     * Traverse a directory and get all files, and add the file into fileList
     *
     * @param node file or directory
     */
    public void generateFileList(File node) {

//        System.out.println("File named "+node.getAbsolutePath().toString());


        //add file only
        if (node.isFile()) {
//            System.out.println("Add file to list");
            fileList.add(node.getPath().toString());
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
//         System.out.println("Node is a directory");

            for (String filename : subNote) {
                generateFileList(new File(node, filename));
            }
        }

    }

    /**
     * Format the file path for zip
     *
     * @param file file path
     * @return Formatted file path
     */
    private String generateZipEntry(String file, String sourceFileName) {
//            System.out.println("zip entry: "+file.substring(sourceFileName.length()+1, file.length()));
        return file.substring(sourceFileName.length() + 1, file.length());
    }

    /*
     * Delete the previous project from the temp folder if one exists.
     * Reference: http://stackoverflow.com/questions/3775694/deleting-folder-from-java
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Unzip the SEAR project to the MyRobotLab temp folder Reference:
     * http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/
     *
     * @param zipFile input zip file
     * @param output zip file output folder
     */
    public void unZipIt(String zipFile) {




        String baseDir = String.format("%1$s%2$s", System.getProperty("user.dir"), File.separator);

//Clear any previous project form the temp folder.
        File previousFile = new File(baseDir + "SEARproject");
        if (previousFile.exists()) {
            System.out.println("Deleting File " + baseDir + "SEARproject");
            deleteDir(previousFile);
        }// end if file exists




        byte[] buffer = new byte[1024];

        try {

            //create output directory is not exists
            File folder = new File(baseDir);
            if (!folder.exists()) {
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(zipFile));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                String fileName = ze.getName();
                File newFile = new File(baseDir + File.separator + fileName);

                System.out.println("file unzip : " + newFile.getAbsoluteFile());

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

//    	System.out.println("Done");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}//end SEAR.java class

