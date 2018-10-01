package tasks.data.text;

import static omnia.data.stream.Collectors.toImmutableSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import omnia.data.structure.Collection;
import omnia.data.structure.Graph;
import omnia.data.structure.immutable.ImmutableGraph;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import tasks.Task;
import tasks.Task.Id;
import tasks.data.TaskReader;

public final class TextReader implements TaskReader {

  private final Path path;

  public TextReader(Path path) {
    this.path = path;
  }

  @Override
  public Graph<Task> read() throws IOException {
    MutableMap<Id, Task> taskMap = new HashMap<>();
    Collection<Task> parsedTasks =
        Files.lines(path)
            .map(taskString -> parseTaskString(taskString, taskMap))
            .collect(toImmutableSet());

    ImmutableGraph.Builder<Task> graphBuilder = ImmutableGraph.builder();

    // Add individual tasks to graph
    for (Task task : parsedTasks) {
      graphBuilder.addNode(task);
    }

    // Add task dependencies to graph
    for (Task task : parsedTasks) {
      for (Task dependency : task.dependencies()) {
        graphBuilder.addEdge(task, dependency);
      }
    }

    // Return the built graph!
    return graphBuilder.build();
  }

  @Override
  public void close() {
    // no-op since file is opened and closed in read()
  }

  private static Task parseTaskString(String taskString, MutableMap<Id, Task> taskMap) {
    String[] parts = taskString.split(";");
    Task parsedTask =
        Task.builder()
            .id(Id.from(Long.parseLong(parts[0])))
            .name(parts[1])
            .dependencies(
                Arrays.stream(parts[2].split(","))
                    .map(Long::parseLong)
                    .map(Id::from)
                    .map(taskMap::valueOf)
                    .map(Optional::get)
                    .collect(toImmutableSet()))
            .isCompleted(Boolean.parseBoolean(parts[3]))
            .build();

    taskMap.putMapping(parsedTask.id(), parsedTask);
    return parsedTask;
  }
}
