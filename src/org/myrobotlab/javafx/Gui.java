package org.myrobotlab.javafx;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Gui extends Application {
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    primaryStage.setTitle("Hello World!");
    Button btn = new Button();
    btn.setText("Say 'Hello World'");
    btn.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle(ActionEvent event) {
        System.out.println("Hello World!");
      }
    });

    Button btn2 = new Button();
    btn2.setText("Say 'Hello World xxx'");

    // StackPane root = new StackPane();
    // FlowPane root = new FlowPane();
    HBox root = new HBox();
    root.setPadding(new Insets(10, 10, 10, 10));
    root.setSpacing(10);
    root.getChildren().addAll(btn,btn2);
    //root.getChildren().add(btn2);
    // JButton x = new JButton();
    // root.getChildren().add(x);
    primaryStage.setScene(new Scene(root, 300, 250));
    primaryStage.show();

  }
}