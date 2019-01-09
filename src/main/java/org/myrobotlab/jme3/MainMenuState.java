package org.myrobotlab.jme3;

import org.myrobotlab.service.JMonkeyEngine;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
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
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BorderLayout.Position;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.VersionedReference;

import com.simsilica.lemur.style.BaseStyles;

public class MainMenuState extends BaseAppState {
  JMonkeyEngine jme = null;
  transient Jme3App app;
  Node guiNode;
  Label breadCrumbs;
  Container main;
  TabbedPanel tabs;
  VersionedReference<TabbedPanel.Tab> selectionRef;
  private int nextTabNumber = 1;
  private Label statusLabel;
  Label title;

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

    // for (int i = 0; i < 3; i++) {
      addNavTab();
    // }

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

  protected void addNavTab() {
    
    Container contents = new Container();
    Label label = contents.addChild(new Label("children"));
    label.setInsets(new Insets3f(5, 5, 5, 5));
    
    tabs.addTab("nav", contents);
  }

  protected Container createTabContents(String name) {
    Container contents = new Container();
    Label label = contents.addChild(new Label("A test label for tab:" + name + ".\nThere are others like it.\nBut this one is mine."));
    label.setInsets(new Insets3f(5, 5, 5, 5));
    return contents;
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

    /*
     * TextField textfield = new
     * TextField("Hello                                                       "
     * ); // textfield.getActionMap().puts
     * 
     * textfield.getActionMap().put(new KeyAction(KeyInput.KEY_RETURN), new
     * KeyActionListener() {
     * 
     * @Override public void keyAction(TextEntryComponent arg0, KeyAction arg1)
     * { // Enter pressed code System.out.println("The world is yours."); } });
     * textfield.getDocumentModel().end(true);
     * textfield.getDocumentModel().insertNewLine(); // textfield.resetText();
     * 
     * container.addChild(textfield, Position.North);
     */
    // textfield.add
  }

  @Override
  protected void cleanup(Application app) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void onEnable() {
    // Node gui = ((Jme3App)getApplication()).getGuiNode();
    // gui.attachChild(main);
    guiNode.attachChild(main);
    GuiGlobals.getInstance().requestFocus(main);
  }

  @Override
  protected void onDisable() {
    main.removeFromParent();
  }

}
