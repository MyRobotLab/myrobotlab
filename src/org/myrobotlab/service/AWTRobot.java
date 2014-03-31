package org.myrobotlab.service;

//raver1975 was here

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.VideoSources;
import org.slf4j.Logger;

public class AWTRobot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(AWTRobot.class
			.getCanonicalName());
	private Robot robot;
	private Point mousePos;
	private Point oldMousePos;
	private Rectangle maxBounds;
	private Rectangle bounds;
	private Dimension resizedBounds;
	private VideoSources videoSources;
	public final static int BUTTON1_MASK = InputEvent.BUTTON1_MASK;
	public final static int BUTTON2_MASK = InputEvent.BUTTON2_MASK;
	public final static int BUTTON3_MASK = InputEvent.BUTTON3_MASK;
	public final static int SHIFT_DOWN_MASK = KeyEvent.SHIFT_DOWN_MASK;
	public final static int CTRL_DOWN_MASK = KeyEvent.CTRL_DOWN_MASK;
	public final static int ALT_DOWN_MASK = KeyEvent.ALT_DOWN_MASK;

	public class MouseData implements Serializable {
		private static final long serialVersionUID = 1L;
		float x = 0;
		float y = 0;

		public MouseData(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}

	public class KeyData implements Serializable {
		private static final long serialVersionUID = 1L;
		char c = 0;
		int keyCode = 0;
		int modifier = 0;

		public KeyData(char c, int keyCode, int modifier) {
			this.c = c;
			this.keyCode = keyCode;
			this.modifier = modifier;
		}
	}

	public class MouseThread implements Runnable {
		public Thread thread = null;
		private boolean isRunning = true;
		public String name;

		MouseThread(String name) {
			this.name=name;
			thread = new Thread(this, getName() + "_polling_thread");
			thread.start();
		}

		public void run() {
			try {
				while (isRunning) {
					mousePos = MouseInfo.getPointerInfo().getLocation();
					if (!mousePos.equals(oldMousePos)) {
						invoke("publishMouseX", new Float(
								((float) mousePos.x / maxBounds.getWidth())));
						invoke("publishMouseY", new Float(
								((float) mousePos.y / maxBounds.getHeight())));
						invoke("publishMouse",
								new MouseData((float) mousePos.x
										/ (float) maxBounds.getWidth(),
										(float) mousePos.y
												/ (float) maxBounds.getHeight()));
						invoke("publishMouseRaw", new MouseData(
								(float) mousePos.x, (float) mousePos.y));
					}
					oldMousePos = mousePos;
					BufferedImage bi = robot.createScreenCapture(bounds);

					if (resizedBounds != null) {
						// System.out.println(mousePos.,"+
						// mousePos.y*normalizeBounds.height/bounds.height);
						bi = AWTRobot.resize(bi, resizedBounds.width,
								resizedBounds.height);
						java.awt.Graphics g = bi.getGraphics();
						g.setColor(Color.blue);
						g.fillOval(
								((mousePos.x - (int) bounds.getMinX()) * resizedBounds.width)
										/ bounds.width - 2,
								((mousePos.y - (int) bounds.getMinY()) * resizedBounds.height)
										/ bounds.height - 2, 5, 5);
					} else {
						java.awt.Graphics g = bi.getGraphics();
						g.setColor(Color.blue);
						g.fillOval(mousePos.x - (int) bounds.getMinX() - 2,
								mousePos.y - (int) bounds.getMinY() - 2, 5, 5);
					}
					SerializableImage si = new SerializableImage(bi,
							"screenshot");
					invoke("publishDisplay", si);
					//videoSources.put(name, "input",si);
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				log.info("ClockThread interrupt");
				isRunning = false;
			}
		}
	}

	public AWTRobot(String n) {
		super(n);
		videoSources =new VideoSources();
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		maxBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (int j = 0; j < gs.length; j++) {
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration[] gc = gd.getConfigurations();
			for (int i = 0; i < gc.length; i++) {
				maxBounds = maxBounds.union(gc[i].getBounds());
			}
		}
		bounds = (Rectangle) maxBounds.clone();
		resizedBounds = new Dimension(800, 600);
		new MouseThread(this.getName());		
	}

	@Override
	public String getDescription() {
		return "based on Robot class,allows control of mouse/keyboard/screen capture";
	}

	@Override
	public void stopService() {
		super.stopService();
		robot = null;
	}

	@Override
	public void releaseService() {
		super.releaseService();
		robot = null;
	}

	// publish methods -------------------------
	public Float publishMouseX(Float value) {
		return value;
	}

	public Float publishMouseY(Float value) {
		return value;
	}

	public MouseData publishMouseRaw(MouseData value) {
		return value;
	}

	public MouseData publishMouse(MouseData value) {
		return value;
	}

	public SerializableImage publishDisplay(SerializableImage value) {
		return value;
	}

	// end publish methods-----------------------

	private static BufferedImage resize(BufferedImage img, int newW, int newH) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(newW, newH, img.getType());
		Graphics2D g = dimg.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);
		g.dispose();
		return dimg;
	}

	public void setResize(Dimension bounds) {
		resizedBounds = bounds;
	}

	public void setResize(int x, int y) {
		resizedBounds = new Dimension(x, y);
	}

	public Dimension getResize() {
		return resizedBounds;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public Rectangle getMaxBounds() {
		return maxBounds;
	}

	public void setBounds(Rectangle rect) {
//		System.out.println("maxBounds="+maxBounds);
		bounds = maxBounds.intersection(rect);
	}

	public void setBounds(int x, int y, int width, int height) {
//		System.out.println("maxBounds="+maxBounds);
		bounds = maxBounds.intersection(new Rectangle(x, y, width, height));
	}

	public void moveTo(MouseData value) {
		robot.mouseMove((int) (value.x * (float) maxBounds.width),
				(int) (value.y * (float) maxBounds.height));
	}

	// 0.0 - 1.0 scaled to full screen dimenstions
	public void moveTo(float x1, float y1) {
		robot.mouseMove((int) ((float) x1 * (float) maxBounds.width),
				(int) (y1 * (float) maxBounds.height));
	}

	// pixel coordinates
	public void moveTo(int x1, int y1) {
		robot.mouseMove(x1, y1);
	}

	// buttons = use bit mask constants
	public void click(final int buttons) {
		new Thread(new Runnable() {
			public void run() {
				robot.mousePress(buttons);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				robot.mouseRelease(buttons);

			}
		}).start();
	}

	// type a real key
	public void type(final char c) {
		new Thread(new Runnable() {
			public void run() {
				KeyData cc = getKeyEventFromChar(c);
				int keyCode = cc.keyCode;
				if ((cc.keyCode & AWTRobot.SHIFT_DOWN_MASK) == AWTRobot.SHIFT_DOWN_MASK) {
					robot.keyPress(KeyEvent.VK_SHIFT);
				}
				if ((cc.keyCode & AWTRobot.CTRL_DOWN_MASK) == AWTRobot.CTRL_DOWN_MASK) {
					robot.keyPress(KeyEvent.VK_CONTROL);
				}
				if ((cc.keyCode & AWTRobot.ALT_DOWN_MASK) == AWTRobot.ALT_DOWN_MASK) {
					robot.keyPress(KeyEvent.VK_ALT);
				}
				robot.keyPress(keyCode);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				robot.keyRelease(keyCode);
				if ((cc.keyCode & AWTRobot.SHIFT_DOWN_MASK) == AWTRobot.SHIFT_DOWN_MASK) {
					robot.keyRelease(KeyEvent.VK_SHIFT);
				}
				if ((cc.keyCode & AWTRobot.CTRL_DOWN_MASK) == AWTRobot.CTRL_DOWN_MASK) {
					robot.keyRelease(KeyEvent.VK_CONTROL);
				}
				if ((cc.keyCode & AWTRobot.ALT_DOWN_MASK) == AWTRobot.ALT_DOWN_MASK) {
					robot.keyRelease(KeyEvent.VK_ALT);
				}

			}
		}).start();
	}

	// type a string
	public void type(final String s) {
		new Thread(new Runnable() {
			public void run() {
				for (char c : s.toCharArray()) {
					KeyData cc = getKeyEventFromChar(c);
					int bb = cc.keyCode;
					int keyCode = cc.keyCode;
					if ((cc.keyCode & AWTRobot.SHIFT_DOWN_MASK) == AWTRobot.SHIFT_DOWN_MASK) {
						robot.keyPress(KeyEvent.VK_SHIFT);
					}
					if ((cc.keyCode & AWTRobot.CTRL_DOWN_MASK) == AWTRobot.CTRL_DOWN_MASK) {
						robot.keyPress(KeyEvent.VK_CONTROL);
					}
					if ((cc.keyCode & AWTRobot.ALT_DOWN_MASK) == AWTRobot.ALT_DOWN_MASK) {
						robot.keyPress(KeyEvent.VK_ALT);
					}
					robot.keyPress(bb);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					robot.keyRelease(bb);
					if ((cc.keyCode & AWTRobot.SHIFT_DOWN_MASK) == AWTRobot.SHIFT_DOWN_MASK) {
						robot.keyRelease(KeyEvent.VK_SHIFT);
					}
					if ((cc.keyCode & AWTRobot.CTRL_DOWN_MASK) == AWTRobot.CTRL_DOWN_MASK) {
						robot.keyRelease(KeyEvent.VK_CONTROL);
					}
					if ((cc.keyCode & AWTRobot.ALT_DOWN_MASK) == AWTRobot.ALT_DOWN_MASK) {
						robot.keyRelease(KeyEvent.VK_ALT);
					}
				}
			}
		}).start();
	}

	private KeyData getKeyEventFromChar(final char c) {
		int press = -1;
		boolean shift = false;
		switch (c) {
		case 'a':
			press = (KeyEvent.VK_A);
			break;
		case 'b':
			press = (KeyEvent.VK_B);
			break;
		case 'c':
			press = (KeyEvent.VK_C);
			break;
		case 'd':
			press = (KeyEvent.VK_D);
			break;
		case 'e':
			press = (KeyEvent.VK_E);
			break;
		case 'f':
			press = (KeyEvent.VK_F);
			break;
		case 'g':
			press = (KeyEvent.VK_G);
			break;
		case 'h':
			press = (KeyEvent.VK_H);
			break;
		case 'i':
			press = (KeyEvent.VK_I);
			break;
		case 'j':
			press = (KeyEvent.VK_J);
			break;
		case 'k':
			press = (KeyEvent.VK_K);
			break;
		case 'l':
			press = (KeyEvent.VK_L);
			break;
		case 'm':
			press = (KeyEvent.VK_M);
			break;
		case 'n':
			press = (KeyEvent.VK_N);
			break;
		case 'o':
			press = (KeyEvent.VK_O);
			break;
		case 'p':
			press = (KeyEvent.VK_P);
			break;
		case 'q':
			press = (KeyEvent.VK_Q);
			break;
		case 'r':
			press = (KeyEvent.VK_R);
			break;
		case 's':
			press = (KeyEvent.VK_S);
			break;
		case 't':
			press = (KeyEvent.VK_T);
			break;
		case 'u':
			press = (KeyEvent.VK_U);
			break;
		case 'v':
			press = (KeyEvent.VK_V);
			break;
		case 'w':
			press = (KeyEvent.VK_W);
			break;
		case 'x':
			press = (KeyEvent.VK_X);
			break;
		case 'y':
			press = (KeyEvent.VK_Y);
			break;
		case 'z':
			press = (KeyEvent.VK_Z);
			break;
		case 'A':
			press = (KeyEvent.VK_A);
			shift = true;
			break;
		case 'B':
			press = (KeyEvent.VK_B);
			shift = true;
			break;
		case 'C':
			press = (KeyEvent.VK_C);
			shift = true;
			break;
		case 'D':
			press = (KeyEvent.VK_D);
			shift = true;
			break;
		case 'E':
			press = (KeyEvent.VK_E);
			shift = true;
			break;
		case 'F':
			press = (KeyEvent.VK_F);
			shift = true;
			break;
		case 'G':
			press = (KeyEvent.VK_G);
			shift = true;
			break;
		case 'H':
			press = (KeyEvent.VK_H);
			shift = true;
			break;
		case 'I':
			press = (KeyEvent.VK_I);
			shift = true;
			break;
		case 'J':
			press = (KeyEvent.VK_J);
			shift = true;
			break;
		case 'K':
			press = (KeyEvent.VK_K);
			shift = true;
			break;
		case 'L':
			press = (KeyEvent.VK_L);
			shift = true;
			break;
		case 'M':
			press = (KeyEvent.VK_M);
			shift = true;
			break;
		case 'N':
			press = (KeyEvent.VK_N);
			shift = true;
			break;
		case 'O':
			press = (KeyEvent.VK_O);
			shift = true;
			break;
		case 'P':
			press = (KeyEvent.VK_P);
			shift = true;
			break;
		case 'Q':
			press = (KeyEvent.VK_Q);
			shift = true;
			break;
		case 'R':
			press = (KeyEvent.VK_R);
			shift = true;
			break;
		case 'S':
			press = (KeyEvent.VK_S);
			shift = true;
			break;
		case 'T':
			press = (KeyEvent.VK_T);
			shift = true;
			break;
		case 'U':
			press = (KeyEvent.VK_U);
			shift = true;
			break;
		case 'V':
			press = (KeyEvent.VK_V);
			shift = true;
			break;
		case 'W':
			press = (KeyEvent.VK_W);
			shift = true;
			break;
		case 'X':
			press = (KeyEvent.VK_X);
			shift = true;
			break;
		case 'Y':
			press = (KeyEvent.VK_Y);
			shift = true;
			break;
		case 'Z':
			press = (KeyEvent.VK_Z);
			shift = true;
			break;
		case '`':
			press = (KeyEvent.VK_BACK_QUOTE);
			break;
		case '0':
			press = (KeyEvent.VK_0);
			break;
		case '1':
			press = (KeyEvent.VK_1);
			break;
		case '2':
			press = (KeyEvent.VK_2);
			break;
		case '3':
			press = (KeyEvent.VK_3);
			break;
		case '4':
			press = (KeyEvent.VK_4);
			break;
		case '5':
			press = (KeyEvent.VK_5);
			break;
		case '6':
			press = (KeyEvent.VK_6);
			break;
		case '7':
			press = (KeyEvent.VK_7);
			break;
		case '8':
			press = (KeyEvent.VK_8);
			break;
		case '9':
			press = (KeyEvent.VK_9);
			break;
		case '-':
			press = (KeyEvent.VK_MINUS);
			break;
		case '=':
			press = (KeyEvent.VK_EQUALS);
			break;
		case '~':
			press = (KeyEvent.VK_BACK_QUOTE);
			shift = true;
			break;
		case '!':
			press = (KeyEvent.VK_EXCLAMATION_MARK);
			break;
		case '@':
			press = (KeyEvent.VK_AT);
			break;
		case '#':
			press = (KeyEvent.VK_NUMBER_SIGN);
			break;
		case '$':
			press = (KeyEvent.VK_DOLLAR);
			break;
		case '%':
			press = (KeyEvent.VK_5);
			shift = true;
			break;
		case '^':
			press = (KeyEvent.VK_CIRCUMFLEX);
			break;
		case '&':
			press = (KeyEvent.VK_AMPERSAND);
			break;
		case '*':
			press = (KeyEvent.VK_ASTERISK);
			break;
		case '(':
			press = (KeyEvent.VK_LEFT_PARENTHESIS);
			break;
		case ')':
			press = (KeyEvent.VK_RIGHT_PARENTHESIS);
			break;
		case '_':
			press = (KeyEvent.VK_UNDERSCORE);
			break;
		case '+':
			press = (KeyEvent.VK_PLUS);
			break;
		case '\t':
			press = (KeyEvent.VK_TAB);
			break;
		case '\n':
			press = (KeyEvent.VK_ENTER);
			break;
		case '[':
			press = (KeyEvent.VK_OPEN_BRACKET);
			break;
		case ']':
			press = (KeyEvent.VK_CLOSE_BRACKET);
			break;
		case '\\':
			press = (KeyEvent.VK_BACK_SLASH);
			break;
		case '{':
			press = (KeyEvent.VK_OPEN_BRACKET);
			shift = true;
			break;
		case '}':
			press = (KeyEvent.VK_CLOSE_BRACKET);
			shift = true;
			break;
		case '|':
			press = (KeyEvent.VK_BACK_SLASH);
			shift = true;
			break;
		case ';':
			press = (KeyEvent.VK_SEMICOLON);
			break;
		case ':':
			press = (KeyEvent.VK_COLON);
			break;
		case '\'':
			press = (KeyEvent.VK_QUOTE);
			break;
		case '"':
			press = (KeyEvent.VK_QUOTEDBL);
			break;
		case ',':
			press = (KeyEvent.VK_COMMA);
			break;
		case '<':
			press = (KeyEvent.VK_LESS);
			break;
		case '.':
			press = (KeyEvent.VK_PERIOD);
			break;
		case '>':
			press = (KeyEvent.VK_GREATER);
			break;
		case '/':
			press = (KeyEvent.VK_SLASH);
			break;
		case '?':
			press = (KeyEvent.VK_SLASH);
			shift = true;
			break;
		case ' ':
			press = (KeyEvent.VK_SPACE);
			break;
		default:
			throw new IllegalArgumentException("Cannot type character " + c);
		}
		return new KeyData(c, press, shift ? KeyEvent.SHIFT_DOWN_MASK : 0);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		//Runtime.createAndStart("java", "Java");
		Runtime.createAndStart("gui", "GUIService");
		AWTRobot awt = (AWTRobot) Runtime.createAndStart("awt", "AWTRobot");
		awt.setBounds(0, 0, 100, 100);
		TesseractOCR tess = (TesseractOCR) Runtime.createAndStart("tess",
				"TesseractOCR");
		tess.subscribe("publishDisplay", awt.getName(), "OCR");
		// new TIFFImageWriteParam();
		// mouse.setBounds(new Rectangle(500,500));
		// VideoStreamer
		// stream=(VideoStreamer)Runtime.createAndStart("stream","VideoStreamer");
		// stream.subscribe("publishDisplay", mouse.getName(), "publishDisplay",
		// SerializableImage.class);

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}
}
