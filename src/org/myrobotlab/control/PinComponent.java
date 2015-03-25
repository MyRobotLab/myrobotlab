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

package org.myrobotlab.control;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import org.myrobotlab.control.widget.DigitalButton;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Pin;
import org.slf4j.Logger;

public class PinComponent {

	public final static Logger log = LoggerFactory.getLogger(PinComponent.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	public final String boundServiceName;

	public final int pinNumber;
	public boolean isAnalog = false;
	boolean isPWM = false;
	JLabel counter = null;

	public DigitalButton inOut = null;
	public DigitalButton onOff = null;
	public DigitalButton activeInActive = null;
	public DigitalButton trace = null;

	public JSlider pwmSlider = null;

	JLabel pinLabel = null;
	public JLabel data = null;

	boolean isVertical = false;
	public final Service myService;

	// types of DigitalButtons
	public final static int TYPE_ONOFF = 0;
	public final static int TYPE_INOUT = 1;
	public final static int TYPE_ACTIVEINACTIVE = 2;
	public final static int TYPE_TRACE = 3;

	// values
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;
	public static final int OUTPUT = 0x1;
	public static final int INPUT = 0x0;

	public PinComponent(Service myService, String boundServiceName, int pinNumber, boolean isPWM, boolean isAnalog, boolean isVertical) {
		this.boundServiceName = boundServiceName;
		this.isAnalog = isAnalog;
		this.isPWM = isPWM;
		this.pinNumber = pinNumber;
		this.myService = myService;
		this.isVertical = isVertical;

		data = new JLabel("0");

		pinLabel = new JLabel("pin " + pinNumber);
		pinLabel.setPreferredSize(new Dimension(40, 13));

		if (!isVertical) {
			inOut = new DigitalButton(this, "out", Color.decode("0x418dd9"), Color.white, "in", Color.white, Color.decode("0x418dd9"), TYPE_INOUT);

			onOff = new DigitalButton(this, "off", Color.gray, Color.white, "on", Color.green, Color.black, TYPE_ONOFF);

			activeInActive = new DigitalButton(this, "inactive", Color.decode("0x418dd9"), Color.white, "active", Color.red, Color.white, TYPE_ACTIVEINACTIVE);
		} else {
			inOut = new DigitalButton(this, "out", Util.getImageIcon("out.png"), "in", Util.getImageIcon("in.png"), TYPE_INOUT);

			onOff = new DigitalButton(this, "off", Util.getImageIcon("off.png"), "on", Util.getImageIcon("on.png"), TYPE_ONOFF);

			activeInActive = new DigitalButton(this, "inactive", Util.getImageIcon("Arduino/inactive.png"), "active", Util.getImageIcon("Arduino/active.png"), TYPE_ACTIVEINACTIVE);

		}
		if (isAnalog) {
			trace = new DigitalButton(this, "A" + (pinNumber), "offTrace", Color.decode("0x418dd9"), Color.white, "A" + (pinNumber), "onTrace", Color.red, Color.white, TYPE_TRACE);
		} else {
			trace = new DigitalButton(this, "D" + (pinNumber), "offTrace", Color.decode("0x418dd9"), Color.white, "D" + (pinNumber), "onTrace", Color.red, Color.white, TYPE_TRACE);

		}

		if (isPWM) {
			pwmSlider = getPWMSlider();
		}

	}

	public PinComponent(Service myService, String boundServiceName, Pin pin, boolean isVertical) {
		this(myService, boundServiceName, pin.pin, pin.type == Pin.PWM_VALUE, pin.type == Pin.ANALOG_VALUE, isVertical);
	}

	// TODO - remove
	private JSlider getPWMSlider() {
		if (pwmSlider == null) {
			int orientation = (isVertical) ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL;
			pwmSlider = new JSlider(orientation, 0, 255, 0);
			pwmSlider.setOpaque(false);
			pwmSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					data.setText("" + pwmSlider.getValue());
					if (myService != null) {
						myService.send(boundServiceName, "analogWrite", pinNumber, pwmSlider.getValue());
					} else {
						log.error("can not send message myService is null");
					}
				}
			});

		}
		return pwmSlider;
	}

}