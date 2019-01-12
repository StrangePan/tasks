package tasks.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {

  @Override
  public void start(Stage primaryStage) {
    Text text = new Text("Hello, World!");
    text.setFont(new Font(40));

    StackPane stackPane = new StackPane();
    stackPane.getChildren().add(text);
    stackPane.setAlignment(Pos.CENTER_LEFT);

    Scene scene = new Scene(stackPane);

    primaryStage.setTitle("Hello, World!");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  public static void main(String[] args) {
    Application.launch(args);
  }
}
