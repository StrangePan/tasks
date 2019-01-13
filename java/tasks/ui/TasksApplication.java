package tasks.ui;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import omnia.algorithm.GraphAlgorithms;
import omnia.data.structure.Collection;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.HomogeneousPair;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import tasks.Task;
import tasks.io.File;
import tasks.io.TaskStore;

public final class TasksApplication extends javafx.application.Application {

  private final File file = File.fromPath("tmp.tasks");
  private final TaskStore taskStore = new TaskStore(file);
  private DirectedGraph<Task> tasks;

  @Override
  public void init() {
    Collection<Task> taskCollection = taskStore.retrieveBlocking();
    ImmutableDirectedGraph.Builder<Task> builder = ImmutableDirectedGraph.builder();

    // Add edges
    taskCollection.stream()
        .flatMap(
            task ->
                task.dependencies()
                    .stream()
                    .map(dependency -> HomogeneousPair.of(task, dependency)))
        .forEach(
            pair -> builder.addEdge(pair.first(), pair.second()));

    // Add isolated nodes
    taskCollection.stream()
        .filter(task -> !task.dependencies().isPopulated())
        .forEach(builder::addNode);

    tasks = builder.build();
  }

  @Override
  public void start(Stage primaryStage) {
    buildUi(primaryStage);
  }

  private void buildUi(Stage stage) {
    Parent mainNode = buildBorderPane();

    stage.setScene(new Scene(mainNode));
    stage.show();
  }

  private BorderPane buildBorderPane() {
    BorderPane borderPane = new BorderPane();
    borderPane.setLeft(buildLeftPane());
    borderPane.setCenter(buildCenterPane());
    return borderPane;
  }

  private Node buildLeftPane() {
    BorderPane borderPane = new BorderPane();
    borderPane.setTop(buildLeftPaneTitle());
    borderPane.setCenter(buildLeftScrollPane());
    borderPane.setMinWidth(200);
    return borderPane;
  }

  private Node buildLeftPaneTitle() {
    Text text = new Text("next up:");
    text.setTextAlignment(TextAlignment.CENTER);
    return text;
  }

  private Node buildLeftScrollPane() {
    ScrollPane scrollPane = new ScrollPane();
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setContent(buildLeftScrollPaneContents());
    return scrollPane;
  }

  private Node buildLeftScrollPaneContents() {
    VBox vbox = new VBox();
    vbox.getChildren()
        .addAll(
            Stream.concat(
                    GraphAlgorithms.sinkElements(tasks).stream(),
                    GraphAlgorithms.isolatedElements(tasks).stream())
                .filter(task -> !task.isCompleted())
                .map(this::buildNodeForTask)
                .collect(Collectors.toSet()));
    return vbox;
  }

  private Node buildNodeForTask(Task task) {
    return new Text(task.label());
  }

  private Node buildCenterPane() {
    StackPane stackPane = new StackPane();
    stackPane.getChildren().add(buildCenterPaneMsg());
    stackPane.setAlignment(Pos.CENTER);
    stackPane.setMinWidth(400);
    stackPane.setMinHeight(400);
    return stackPane;
  }

  private Node buildCenterPaneMsg() {
    return new Text("work in progress");
  }

  public void loadTasks(TaskStore taskStore) {
    taskStore.retrieveBlocking();
  }

  public static void main(String[] args) {
    TasksApplication.launch(args);
  }
}
