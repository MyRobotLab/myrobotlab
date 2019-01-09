package org.myrobotlab.jme3;

import org.myrobotlab.service.JMonkeyEngine;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BorderLayout.Position;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.style.BaseStyles;

public class MainMenuState extends BaseAppState {
  transient JMonkeyEngine jme = null;
  transient Jme3App app;
  transient Container main;
  transient Label breadCrumbs;
  transient Node guiNode;

  VersionedReference<TabbedPanel.Tab> selectionRef;
  private Label statusLabel;

  TabbedPanel tabs;

  Label title;

  TextField x;
  TextField y;
  TextField z;

  TextField roll;
  TextField pitch;
  TextField yaw;
  
  Button update;

  /**
   * FYI - this is all initialized JMEMain thread ..
   */
  public MainMenuState(JMonkeyEngine jme) {
    this.jme = jme;
    app = jme.getApp();
    guiNode = app.getGuiNode();
    // Initialize the globals access so that the default
    // components can find what they need.
    GuiGlobals.initialize(app);

    // Load the 'glass' style
    BaseStyles.loadGlassStyle();

    // Set 'glass' as the default style when not specified
    GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
  }

  protected void addInfoTab() {

    Container contents = new Container();

    contents.addChild(new Label("x:"), 0, 0);
    contents.addChild(x, 0, 1);
    contents.addChild(new Label("y:"), 0, 2);
    contents.addChild(y, 0, 3);
    contents.addChild(new Label("y:"), 0, 4);
    contents.addChild(z, 0, 5);

    contents.addChild(new Label("pitch:"), 1, 0);
    contents.addChild(new Label("roll:"), 1, 2);
    contents.addChild(new Label("yaw:"), 1, 4);
    contents.addChild(pitch, 1, 1);
    contents.addChild(roll, 1, 3);
    contents.addChild(yaw, 1, 5);
    
    contents.addChild(update, 2, 5);

    // Label label = contents.addChild(new Label("children"));

    // label.setInsets(new Insets3f(5, 5, 5, 5));

    tabs.addTab("info", contents);
  }

  protected void addNavTab() {

    Container contents = new Container();
    Label label = contents.addChild(new Label("children"));
    contents.addChild(new Label("x:"));
    label.setInsets(new Insets3f(5, 5, 5, 5));

    tabs.addTab("nav", contents);
  }

  @Override
  protected void cleanup(Application app) {
    // TODO Auto-generated method stub

  }

  protected Container createTabContents(String name) {
    Container contents = new Container();
    Label label = contents.addChild(new Label("A test label for tab:" + name + ".\nThere are others like it.\nBut this one is mine."));
    label.setInsets(new Insets3f(5, 5, 5, 5));
    return contents;
  }

  @Override
  protected void initialize(Application appx) {
    main = new Container();
    main.setLayout(new BorderLayout());
    guiNode = app.getGuiNode();

    guiNode.attachChild(main);
    // main.setPreferredSize(new Vector3f(300, jme.getSettings().getWidth(),
    // 30));

    // Put it somewhere that we will see it
    // Note: Lemur GUI elements grow down from the upper left corner.
    main.setLocalTranslation(10, 300, 0);

    Container north = new Container();
    Container center = new Container();
    Container south = new Container();

    main.addChild(north, Position.North);
    main.addChild(center, Position.Center);
    main.addChild(south, Position.South);

    Label title = north.addChild(new Label("selected"));
    title.setFontSize(16);
    title.setInsets(new Insets3f(10, 10, 0, 10));

    breadCrumbs = new Label("                                        ");
    north.addChild(breadCrumbs);

    Button nav = center.addChild(new Button("nav"));
    Button floor = center.addChild(new Button("floor"));

    nav.addClickCommands(new Command<Button>() {
      @Override
      public void execute(Button source) {
        System.out.println("nav mode");
      }
    });

  }

  public void loadGui() {
    main = new Container();
    main.setLayout(new BorderLayout());

    guiNode.attachChild(main);
    // main.setPreferredSize(new Vector3f(300, jme.getSettings().getWidth(),
    // 30));

    // Put it somewhere that we will see it
    // Note: Lemur GUI elements grow down from the upper left corner.
    main.setLocalTranslation(10, 300, 0);

    Container north = new Container();
    Container center = new Container();
    Container south = new Container();

    main.addChild(north, Position.North);
    main.addChild(center, Position.Center);
    main.addChild(south, Position.South);

    title = north.addChild(new Label("selected"));
    title.setFontSize(16);
    title.setInsets(new Insets3f(10, 10, 0, 10));

    breadCrumbs = new Label("                                        ");
    north.addChild(breadCrumbs);

    Button nav = center.addChild(new Button("nav"));
    Button floor = center.addChild(new Button("floor"));

    nav.addClickCommands(new Command<Button>() {
      @Override
      public void execute(Button source) {
        System.out.println("nav mode");
      }
    });

    tabs = south.addChild(new TabbedPanel());
    tabs.setInsets(new Insets3f(5, 5, 5, 5));
    selectionRef = tabs.getSelectionModel().createReference();
    
    x = new TextField("0.000");
    y = new TextField("0.000");
    z = new TextField("0.000");

    roll = new TextField("0.000");
    pitch = new TextField("0.000");
    yaw = new TextField("0.000");

    update = new Button("update");

    addInfoTab();
    addNavTab();

    statusLabel = south.addChild(new Label("Status"));
    statusLabel.setInsets(new Insets3f(2, 5, 2, 5));

    // Add some actions that will manipulate the document model independently
    // of the text field
    Container buttons = south.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
    buttons.setInsets(new Insets3f(5, 5, 5, 5));
    buttons.addChild(new Button("first"));
    buttons.addChild(new Button("add"));
    buttons.addChild(new Button("insert"));
    buttons.addChild(new Button("remove"));
    buttons.addChild(new Button("last"));

  }

  @Override
  protected void onDisable() {
    main.removeFromParent();
  }

  @Override
  protected void onEnable() {
    // Node gui = ((Jme3App)getApplication()).getGuiNode();
    // gui.attachChild(main);
    guiNode.attachChild(main);
    GuiGlobals.getInstance().requestFocus(main);
  }

  public void putText(Spatial spatial) {
    Vector3f xyz = spatial.getWorldTranslation();
    Quaternion q = spatial.getLocalRotation();
    float[] angles = new float[3]; // yaw, roll, pitch
    q.toAngles(angles);
    
    x.setText(String.format("%.3f", xyz.x));
    y.setText(String.format("%.3f", xyz.y));
    z.setText(String.format("%.3f", xyz.z));
   
    // pitch.setText("%.3f", angles[0] * 180);
    
    String type = (spatial instanceof Node) ? "Node" : "Geometry";

    title.setText(String.format("%s : %s", type, spatial.getName()));

    Spatial rootChild = jme.getRootChild(spatial);

    StringBuilder sb = new StringBuilder();
    if (rootChild != null) {
      sb.append(rootChild.getName());
      sb.append(" > ");
      sb.append(spatial.getParent().getName());
      sb.append(" > ");
      sb.append(spatial.getName());
    } else {
      sb.append(spatial.getName());
    }
    breadCrumbs.setText(sb.toString());
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
