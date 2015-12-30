package org.myrobotlab.service;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.DisplayedImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class ImageDisplay extends Service {

	private static final long serialVersionUID = 1L;
	int wOffset = 0;
	int hOffset = 20;
	private static int h,w;
	private static GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	
	public final static Logger log = LoggerFactory.getLogger(ImageDisplay.class);

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			//String path = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQJwoVloTUs4cW2uWdsIbP_Fdph0IfEhODRrQoOgFOiYrYj_9J01A";
			//String path2 = "/Users/Sebastien/Pictures/scan10.jpeg";
			String path3 = "http://vignette3.wikia.nocookie.net/jadensadventures/images/5/54/Pokemon-Ash-s-Pikachu-Riley-Sir-Aaron-s-Lucarios-pokemon-guys-10262907-563-579.jpg/revision/latest?cb=20120902022940";

			ImageDisplay imageDisplay = (ImageDisplay) Runtime.start("ImageDisplay", "ImageDisplay");
			Runtime.start("gui", "GUIService");
			Runtime.start("python", "Python");
			//Runtime.start("webgui", "WebGui");
			imageDisplay.display(path3);
			imageDisplay.displayFullScreen("http://cdn.collider.com/wp-content/uploads/2015/06/minions-image-bob-kevin-stuart.jpg");
			//imageDisplay.displayFS(path3);

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public ImageDisplay(String n) {
		super(n);
	}

	//Displays an image.
	//@param source = path.
    public void display(String source){
        DisplayedImage image = new DisplayedImage(source,1.0f);
        log.info("Loading image done");
        buildFrame(image);
    }
    
    //Displays an image in FullScreen mode. 
    //@param source = path.
    public void displayFullScreen(String source){
        DisplayedImage image = new DisplayedImage(source,1.0f,true);
        log.info("Loading image done");
        buildFrameFS(image);
    }
    
    //Displays an image by Fading it in.
    //@param source = path.
    public void displayFadeIn(String source){
        DisplayedImage image = new DisplayedImage(source);
        log.info("Loading image done");
        buildFrame(image);
    }
    
    //Displays a faded image.
    //@param source = path.
    //@param alpha = Value how much the image is faded float from 0.0 to 1.0.
    public void display(String source, float alpha){
        DisplayedImage image = new DisplayedImage(source, alpha);
        log.info("Loading image done");
        buildFrame(image);
    }
    
    //Displays a faded image in FullScreen mode.
    //@param source = path.
    //@param alpha = Value how much the image is faded float from 0.0 to 1.0.
    public void displayFullScreen(String source, float alpha){
        DisplayedImage image = new DisplayedImage(source, alpha, true);
        log.info("Loading image done");
        buildFrameFS(image);
    }
    
    //Displays a resized image in FullScreen mode.
    //@param source = path.
    //@param scaling = scale factor to resize the image.
    public void displayScaled(String source, float scaling){
    	DisplayedImage image = new DisplayedImage(source, 1, scaling);
        log.info("Loading image done");
        buildFrame(image);
    }
    
    //Displays a resized image in FullScreen mode.
    //@param source = path.
    //@param alpha = Value how much the image is faded float from 0.0 to 1.0.
    //@param scaling = scale factor to resize the image.
    public void displayScaled(String source, float alpha, float scaling){
    	DisplayedImage image = new DisplayedImage(source, 1, scaling);
        log.info("Loading image done");
        buildFrame(image);
    }
    
    //builds a JFrame of the right size for the image.
    private void buildFrame(DisplayedImage image){
    	JFrame f = new JFrame();
    	//f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	f.add(image);
    	f.setSize(image.getWidth()+wOffset,image.getHeight()+hOffset);
    	getResolution();
        f.setLocation(w/2-image.getWidth()/2,h/2-(image.getHeight()+hOffset)/2);
        f.setVisible(true);
    }
    
    //builds a JFrame for the FullScreen sized image.
    private void buildFrameFS(DisplayedImage image){
    	final JFrame f = new JFrame();
    	// Exit program on mouse click 
    	f.addMouseListener(new MouseListener() { 
            public void mouseClicked(MouseEvent e) { gd.setFullScreenWindow(null);f.dispose();; } 
            public void mousePressed(MouseEvent e) {} 
            public void mouseReleased(MouseEvent e) {} 
            public void mouseEntered(MouseEvent e) {} 
            public void mouseExited(MouseEvent e) {} } 
        );
    	gd.setFullScreenWindow(f);
    	f.add(image);
    	//It sets the size of the Frame to the size of the picture, if not it will be build a boarder to the right end of the screen.
    	f.setSize(image.getWidth()+wOffset,image.getHeight()+hOffset);
    	getResolution();
    	f.setLocation(image.getwOffset(),image.gethOffset());
        f.setVisible(true);
    }
    
    //Exits the Fullscreen mode.
    public void exitFS(){
    	gd.setFullScreenWindow(null);
    }
    
    //Getting display resolution: width and height 
    public static void getResolution(){
    w = gd.getDisplayMode().getWidth();
    h = gd.getDisplayMode().getHeight();
    log.info("Display resolution: " + w + "x" + String.valueOf(h));
    }
    
    //Returns the Width-factor of the DisplayResolution.
    public static int getResolutionOfW(){
    	return gd.getDisplayMode().getWidth();
    }
    
  //Returns the Height-factor of the DisplayResolution.
    public static int getResolutionOfH(){
    	return gd.getDisplayMode().getHeight();
    }
    

	@Override
	public String[] getCategories() {
		return new String[] { "general" };
	}

	@Override
	public String getDescription() {
		return "Service to Display Images";
	}
}

