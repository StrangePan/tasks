package tasks.data.text;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableSet;
import tasks.Task;
import tasks.Task.Id;
import tasks.data.TaskWriter;

public final class TextWriter implements TaskWriter {

  private final Path path;

  public TextWriter(Path path) {
    this.path = requireNonNull(path);
  }

  @Override
  public void write(Collection<Task> tasks) throws IOException {
    // Flatten graph to a list of tasks sorted by tasks with no dependencies at the front
    List<Task> sortedTasks = sortTasks(tasks);

    Files.write(
        path, (Iterable<String>) sortedTasks.stream().map(TextWriter::createTaskString)::iterator);
  }

  @Override
  public void close() {
    // no-op since file is opened and closed in read()
  }

  private static List<Task> sortTasks(Collection<Task> tasks) {
    ImmutableList.Builder<Task> sortedTasks = ImmutableList.builder();
    MutableSet<Id> seenIds = new HashSet<>();
    for (Task task : tasks) {
      sortedTasks.addAll(generateTaskList(task, seenIds));
    }
    return sortedTasks.build();
  }

  private static List<Task> generateTaskList(Task task, MutableSet<Id> seenIds) {
    ImmutableList.Builder<Task> taskList = ImmutableList.builder();
    if (!seenIds.contains(task.id())) {
      seenIds.add(task.id());
      for (Task dependency : task.dependencies()) {
        taskList.addAll(generateTaskList(dependency, seenIds));
      }
      taskList.add(task);
    }
    return taskList.build();
  }

  private static String createTaskString(Task task) {
    return new StringBuilder()
        .append(task.id())
        .append(';')
        .append(Utils.escapist().escape(task.label()))
        .append(';')
        .append(
            task.dependencies()
                .stream()
                .map(Task::id)
                .map(Id::asLong)
                .map(l -> Long.toString(l))
                .collect(Collectors.joining(",")))
        .append(';')
        .append(Boolean.toString(task.isCompleted()))
        .toString();
  }
}
