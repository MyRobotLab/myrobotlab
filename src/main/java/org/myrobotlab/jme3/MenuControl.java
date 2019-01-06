package org.myrobotlab.jme3;

import org.myrobotlab.service.JMonkeyEngine;

import com.jme3.input.KeyInput;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BorderLayout.Position;
import com.simsilica.lemur.component.TextEntryComponent;
import com.simsilica.lemur.event.KeyAction;
import com.simsilica.lemur.event.KeyActionListener;
import com.simsilica.lemur.style.BaseStyles;

public class MenuControl {
  JMonkeyEngine jme = null;
  transient Jme3App app;
  Node guiNode;  
  Label breadCrumbs;

  public MenuControl(JMonkeyEngine jme) {
    this.jme = jme;
    app = jme.getApp();
    guiNode = app.getGuiNode();
    // Initialize the globals access so that the defualt
    // components can find what they need.
    GuiGlobals.initialize(app);

    // Load the 'glass' style
    BaseStyles.loadGlassStyle();

    // Set 'glass' as the default style when not specified
    GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
  }
  
  Container main;
  

  public void loadGui() {
    main = new Container();
    main.setLayout(new BorderLayout());
    guiNode.attachChild(main);
    // main.setPreferredSize(new Vector3f(300, jme.getSettings().getWidth(), 30));

    // Put it somewhere that we will see it
    // Note: Lemur GUI elements grow down from the upper left corner.
    main.setLocalTranslation(300, 300, 0);
    
    Container north = new Container();
    Container center = new Container();
    Container south = new Container();
    
    main.addChild(north, Position.North);
    main.addChild(center, Position.Center);
    main.addChild(south, Position.South);
     
    breadCrumbs = new Label("                                        ");
    north.addChild(breadCrumbs, 0,0);    
    
    Button nav = center.addChild(new Button("nav"), 0,0);
    Button floor = center.addChild(new Button("floor"), 0,1);

    nav.addClickCommands(new Command<Button>() {
      @Override
      public void execute(Button source) {
        System.out.println("nav mode");
      }
    });

    /*
    TextField textfield = new TextField("Hello                                                       ");
    // textfield.getActionMap().puts
    
    textfield.getActionMap().put(new KeyAction(KeyInput.KEY_RETURN), new KeyActionListener() {
      @Override
      public void keyAction(TextEntryComponent arg0, KeyAction arg1) {
        // Enter pressed code
        System.out.println("The world is yours.");
      }
    });
    textfield.getDocumentModel().end(true);
    textfield.getDocumentModel().insertNewLine();
    // textfield.resetText();

    container.addChild(textfield, Position.North);
    */
    // textfield.add
  }
  
  public void setBreadCrumb(Spatial spatial) {
    Spatial rootChild = jme.getRootChild(spatial);
    StringBuilder sb = new StringBuilder();
    sb.append(rootChild.getName());
    sb.append(" > ");
    sb.append(spatial.getParent().getName());
    sb.append(" > ");
    sb.append(spatial.getName());
    breadCrumbs.setText(sb.toString());
  }
  
  
  
}
