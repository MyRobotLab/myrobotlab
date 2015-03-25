package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.repo.Repo;
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
	transient Scanner scanner = new Scanner();

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {
			Runtime runtime = Runtime.getInstance();
			Repo repo = runtime.getRepo();
			repo.install("TopCodes");

			TopCodes topcodes = (TopCodes) Runtime.start("topcode", "TopCodes");

			topcodes.startService();
			List<TopCode> codes = topcodes.scan("topcodetest.png");

			if (codes.size() == 0) {
				log.info("no codes found");
			}
			for (int i = 0; i < codes.size(); ++i) {
				TopCode code = codes.get(i);
				log.info(String.format("number %d code %d x %f y %f diameter %f", i, code.getCode(), code.getCenterX(), code.getCenterY(), code.getDiameter()));
			}

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public TopCodes(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "video", "sensor" };
	}

	@Override
	public String getDescription() {
		return "used as a general topcodes";
	}

	public List<TopCode> scan(BufferedImage img) {
		return scanner.scan(img);
	}

	public List<TopCode> scan(String filename) {
		try {
			BufferedImage img;
			img = ImageIO.read(new File(filename));
			return scanner.scan(img);
		} catch (IOException e) {
			error(e.getMessage());
			Logging.logError(e);
		}

		return null;
	}

	@Override
	public Status test() {
		Status status = super.test();

		return status;
	}
}
