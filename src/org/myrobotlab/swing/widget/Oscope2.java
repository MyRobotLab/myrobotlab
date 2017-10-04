package org.myrobotlab.swing.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.swing.ServiceGui;

public class Oscope2 extends ServiceGui implements ActionListener {

	int w = 600;
	int h = 100;

	// JPanel Oscope2Panel = new JPanel(new BorderLayout());
	// TODO - bit-blit 2 video panels
	// VideoWidget video;
	GridBagConstraints bgc = new GridBagConstraints();
	GridBagConstraints sgc = new GridBagConstraints();
	JPanel buttonPanel = new JPanel(new GridBagLayout());
	JPanel screenPanel = new JPanel(new GridBagLayout());

	Map<String, Trace> traces = new HashMap<String, Trace>();
	Map<Integer, Trace> traceIndex = new HashMap<Integer, Trace>();
	float gradient;

	// raster level access
	protected int[] data;

	/**
	 * this class extends JPanel so that we can control the painting
	 * 
	 * @author GroG
	 *
	 */

	BufferedImage b0;

	Graphics2D g;

	int timeDivisor = 1;

	// http://stackoverflow.com/questions/3256269/jtextfields-on-top-of-active-drawing-on-jpanel-threading-problems/3256941#3256941

	// https://www.nutt.net/create-scrolling-background-java/

	// g1 = b1.createGraphics();
	// http://stackoverflow.com/questions/15511282/how-can-i-make-a-java-swing-animation-smoother
	// http://stackoverflow.com/questions/2063607/java-panel-double-buffering
	// http://gamedev.stackexchange.com/questions/70711/how-do-i-double-buffer-renders-to-a-jpanel
	// http://stackoverflow.com/questions/4430356/java-how-to-do-double-buffering-in-swing
	// http://www.cokeandcode.com/info/tut2d.html

	public class Trace implements ActionListener {
		JButton b;
		PinDefinition pinDef;
		Color color;
		Oscope2 Oscope2;
		ActionListener relay;

		JLabel pinDisplay;
		JPanel screenDisplay = new JPanel(new BorderLayout());
		JLabel pinInfo = new JLabel();

		int width = 600;
		int height = 100;

		int b0Xpos;
		int x;
		int y;

		PinData pinData;
		PinData lastPinData;

		public Trace(Oscope2 Oscope2, PinDefinition pinDef, Color hsv) {
			this.Oscope2 = Oscope2;
			this.pinDef = pinDef;
			b = new JButton(pinDef.getPinName());
			b.setMargin(new Insets(0, 0, 0, 0));
			b.setBorder(null);
			b.setPreferredSize(new Dimension(30, 30));
			b.setBackground(hsv);
			color = hsv;
			b.addActionListener(this);
			// relay
			addActionListener(Oscope2);

			pinDisplay = new JLabel();
			b0 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

			g = b0.createGraphics();
			g.setPaint(Color.GREEN);
			g.fillRect(0, 0, b0.getWidth(), b0.getHeight());
			g.setColor(color);

			pinDisplay.setIgnoreRepaint(true);
			// pinDisplay.setDoubleBuffered(false); // weird no access
			// setBackground(Color.GRAY);
			pinDisplay.setOpaque(false);

			screenDisplay.add(pinDisplay, BorderLayout.CENTER);

			JPanel flow = new JPanel();
			flow.add(new JLabel(pinDef.getPinName()));
			flow.add(pinInfo);
			screenDisplay.add(flow, BorderLayout.WEST);
			screenDisplay.setVisible(false);

		}

		public Component getButtonDisplay() {
			return b;
		}

		/**
		 * events from button components are relayed to
		 * 
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			e.setSource(this);
			relay.actionPerformed(e);
		}

		public void addActionListener(ActionListener relay) {
			this.relay = relay;
		}

		public PinDefinition getPinDef() {
			return pinDef;
		}

		public Component getScreenDisplay() {
			return screenDisplay;
		}

		public void setVisible(boolean visible) {
			screenDisplay.setVisible(visible);
		}

		// drawing should be done on this thread !
		// 'displaying' should be done on the paint swing thread
		public void update(PinData pinData) {
			// draw / move 
			// repaint

			b0Xpos += timeDivisor;			
			/*
			 * g2d.drawLine(x, y + h / 2,
			 * xpos,pinData.value * 20 + h / 2);
			 * log.info("{},{} - {},{}", x, y + h / 2,
			 * xpos,pinData.value * 20 + h / 2); if (xpos %
			 * (w*timeDivisor) == 0) { xpos = 0; }
			 */
			// x = xpos;
			// y = pinData.value;
			g = (Graphics2D) pinDisplay.getGraphics();

			pinInfo.setText(pinData.value + "");
			// TODO - NOW IS THE TIME TO UPDATE BUFFERED IMAGES !!!
			// TODO - optimization of shifting the raster data ?

			if (b0Xpos % b0.getWidth() == 0) {
				b0Xpos = 0;
			}


			// update(pinData);
			g.drawImage(b0, b0Xpos, 0, b0.getWidth(), b0.getHeight(), null);

			// draw on our active image (we swap between the two)

			// shift both images and re-draw them

			// ok for ui to update now ..
			pinDisplay.repaint();
			// repaint(new Rectangle(30,30));
			/*
			 * SwingUtilities.invokeLater(new Runnable() { public void run() {
			 * // Here, we can safely update the GUI // because we'll be called
			 * from the // event dispatch thread
			 * pinDisplay.repaint();//("Query: " + queryNo); } });
			 */
		}
	} // end Trace class

	public void onPinList(List<PinDefinition> pinList) {
		traces.clear();
		addButtons(pinList);
	}

	public Oscope2(String boundServiceName, SwingGui myService) {
		super(boundServiceName, myService);
		addTop(new JButton("clear"));
		JPanel flow = new JPanel();
		flow.add(buttonPanel);
		addLeftLine(flow);
		flow = new JPanel();
		flow.add(screenPanel);
		add(flow);

		// since this is a widget - subscribeGui is not auto-magically called
		// by the framework
		subscribeGui();
		// video.displayFrame(sensorImage);
	}

	public void subscribeGui() {
		subscribe("publishPinDefinition");
		subscribe("publishPinArray");
		subscribe("getPinList");
	}

	public void unsubscribeGui() {
		unsubscribe("publishPinDefinition");
		unsubscribe("publishPinArray");
		unsubscribe("getPinList");
	}

	// enabled -> true | false
	public void onPinDefinition(PinDefinition pinDef) {
		updatePinDisplay(pinDef);
	}

	public void updatePinDisplay(PinDefinition pinDef) {

	}

	public void addButtons(List<PinDefinition> pinList) {

		gradient = 1.0f / pinList.size();

		bgc.gridy = bgc.gridx = 0;
		sgc.gridy = sgc.gridx = 0;

		// gc.fill = GridBagConstraints.BOTH; // ???
		bgc.fill = GridBagConstraints.HORIZONTAL;
		sgc.fill = GridBagConstraints.HORIZONTAL;
		bgc.weighty = bgc.weightx = 1;
		sgc.weighty = sgc.weighty = 1;

		// List<PinDefinition> pinList = pinDefs.getList();
		for (int i = 0; i < pinList.size(); ++i) {
			PinDefinition pinDef = pinList.get(i);
			Color hsv = new Color(Color.HSBtoRGB((i * (gradient)), 0.5f, 1.0f));
			Trace trace = new Trace(this, pinDef, hsv);
			traces.put(pinDef.getPinName(), trace);
			traceIndex.put(pinDef.getAddress(), trace);
			buttonPanel.add(trace.getButtonDisplay(), bgc);
			screenPanel.add(trace.getScreenDisplay(), bgc);
			bgc.gridx++;
			sgc.gridy++;

			if (bgc.gridx % 2 == 0) {
				bgc.gridx = 0;
				bgc.gridy++;
			}

		}

	}

	// TODO - is this optimized raster info ?
	public void initialize() {
		data = new int[h * w];

		// Fill data array with pure solid black
		Arrays.fill(data, 0xff000000);

		// Java's endless black magic to get it working
		DataBufferInt db = new DataBufferInt(data, h * w);
		ColorModel cm = ColorModel.getRGBdefault();
		SampleModel sm = cm.createCompatibleSampleModel(w, h);
		WritableRaster wr = Raster.createWritableRaster(sm, db, null);
		BufferedImage buffer = new BufferedImage(cm, wr, false, null);
		SerializableImage sensorImage = new SerializableImage(buffer, "Oscope2");
	}

	// ??? - onPaint in extended JPanel or JLabel ???
	public void onPinArray(final PinData[] data) {
		// optimize - test if visible
		for (PinData pinData : data) {

			Trace trace = traceIndex.get(pinData.address);
			trace.update(pinData);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Add relay ?
		Object o = e.getSource();
		if (o instanceof Trace) {
			Trace b = (Trace) o;
			PinDefinition pinDef = b.getPinDef();
			if (pinDef.isEnabled()) {
				send("disablePin", pinDef.getAddress());
				b.setVisible(false);
			} else {
				send("enablePin", pinDef.getAddress());
				b.setVisible(true);
			}
		}
	}
}
