package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import topcodes.Scanner;
import topcodes.TopCode;


public class TopCodes extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(TopCodes.class.getCanonicalName());
	Scanner scanner = new Scanner();
	
	public TopCodes(String n) {
		super(n);	
	}

	@Override
	public String getDescription() {
		return "used as a general topcodes";
	}

	
	public List<TopCode> scan(BufferedImage img)
	{
		return scanner.scan(img);
	}

	public List<TopCode> scan(String filename)
	{
		try {
			BufferedImage img;
			img = ImageIO.read(new File(filename));
			return scanner.scan(img);
		} catch (IOException e) {
			error(e.getMessage());
			Logging.logException(e);
		}
		
		return null;
	}

	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		TopCodes topcodes = new TopCodes("topcodes");
		topcodes.startService();			
		List<TopCode> codes = topcodes.scan("topcodetest.png");
		
		if (codes.size() == 0)
		{
			log.info("no codes found");
		}
		for (int i = 0; i < codes.size(); ++i)
		{
			TopCode code = codes.get(i);
			log.info(String.format("number %d code %d x %f y %f diameter %f", i, code.getCode(), code.getCenterX(), code.getCenterY(), code.getDiameter()));
		}
		
	}


}
