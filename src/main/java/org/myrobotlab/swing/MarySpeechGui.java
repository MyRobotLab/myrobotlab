/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JPanel;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.MarySpeech;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.abstracts.AbstractSpeechSynthesisGui;
import org.slf4j.Logger;

public class MarySpeechGui extends AbstractSpeechSynthesisGui implements ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MarySpeechGui.class);

  // used for effects like in mary
  protected final JPanel EffectpanelTLeft = new JPanel();
  protected final JPanel EffectpanelTRight = new JPanel();
  protected final JPanel EffectpanelBLeft = new JPanel();
  protected final JPanel EffectpanelBRight = new JPanel();

  public MarySpeechGui(final String boundServiceName, final SwingGui myService) throws IOException {
    super(boundServiceName, myService);
    /*
     * Runtime.getInstance(); MarySpeech mary = (MarySpeech)
     * Runtime.getService(boundServiceName); EffectpanelTLeft.setLayout(new
     * GridLayout(1, 1, 0, 0)); EffectpanelTRight.setLayout(new GridLayout(1, 1,
     * 0, 0)); EffectpanelBLeft.setLayout(new GridLayout(1, 1, 0, 0));
     * EffectpanelBRight.setLayout(new GridLayout(1, 1, 0, 0));
     * 
     * mary.getEffectsList().forEach((k, v) -> comboEffects.addItem(k));
     * 
     * EffectpanelTLeft.add(ComboEffectLabel);
     * EffectpanelTLeft.add(comboEffects);
     * EffectpanelBRight.add(selectedEffects);
     * 
     * EffectpanelTRight.add(ComboEffectLabel2);
     * EffectpanelTRight.add(effetsParameters); EffectpanelBLeft.add(addEffect);
     * EffectpanelBLeft.add(updateEffect); speechGuiPanel.add(EffectpanelTLeft);
     * speechGuiPanel.add(EffectpanelTRight);
     * speechGuiPanel.add(EffectpanelBLeft);
     * speechGuiPanel.add(EffectpanelBRight);
     */

  }

}
