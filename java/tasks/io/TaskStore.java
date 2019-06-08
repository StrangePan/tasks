package tasks.io;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import omnia.data.structure.Collection;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.HashSet;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.mutable.MutableSet;
import omnia.string.Escapist;
import omnia.string.SimpleEscapist;
import tasks.Task;

public class TaskStore implements Store<Collection<Task>> {

  private final File file;

  public TaskStore(File file) {
    this.file = requireNonNull(file);
  }

  @Override
  public synchronized Collection<Task> retrieveBlocking() {
    try (TextReader reader = new TextReader(file.openInputStream())) {
      return reader.read();
    }
  }

  @Override
  public synchronized void storeBlocking(Collection<Task> data) {
    requireNonNull(data);
    try (TextWriter writer = new TextWriter(file.openOutputStream())) {
      writer.write(data);
    }
  }

  private static final Escapist ESCAPIST =
      SimpleEscapist.builder()
          .escapeCharacter('\\')
          .addEscapedCharacter(';')
          .addEscapedReplacement('\n', 'n')
          .build();

  /** Escapist for text file io. */
  private static Escapist escapist() {
    return ESCAPIST;
  }

  /** Responsible for parsing file contents as task collection. */
  private static final class TextReader implements AutoCloseable {

    private final Scanner scanner;

    TextReader(InputStream inputStream) {
      this.scanner = new Scanner(requireNonNull(inputStream));
    }

    Collection<Task> read() {
      MutableMap<Task.Id, Task> taskMap = new HashMap<>();
      while (scanner.hasNextLine()) {
        parseTaskString(scanner.nextLine(), taskMap);
      }
      return taskMap.values();
    }

    @Override
    public void close() {
      scanner.close();
    }

    private static void parseTaskString(String taskString, MutableMap<Task.Id, Task> taskMap) {
      String[] parts = taskString.split(";");
      Task parsedTask =
          Task.builder()
              .id(Task.Id.from(Long.parseLong(parts[0])))
              .label(escapist().unescape(parts[1]))
              .dependencies(
                  Arrays.stream(parts[2].split(","))
                      .filter(s -> !s.isEmpty())
                      .map(Long::parseLong)
                      .map(Task.Id::from)
                      .map(taskMap::valueOf)
                      .map(Optional::get)
                      .collect(toImmutableSet()))
              .isCompleted(Boolean.parseBoolean(parts[3]))
              .build();

      taskMap.putMapping(parsedTask.id(), parsedTask);
    }
  }
  
  /** Responsible for writing a collection of tasks to a text file. */
  private static final class TextWriter implements AutoCloseable {
    
    private final OutputStream outputStream;

    TextWriter(OutputStream outputStream) {
      this.outputStream = requireNonNull(outputStream);
    }

    void write(Collection<Task> tasks) {
      // Flatten graph to a list of tasks sorted by tasks with no dependencies at the front
      List<Task> sortedTasks = generateTasks(tasks);

      sortedTasks.stream()
          .map(TextWriter::createTaskString)
          .forEachOrdered(this::writeToStream);
    }

    private void writeToStream(String line) {
      try {
        outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public void close() {
      // no-op since file is opened and closed in write
    }

    private static List<Task> generateTasks(Collection<Task> tasks) {
      ImmutableList.Builder<Task> sortedTasks = ImmutableList.builder();
      MutableSet<Task.Id> seenIds = new HashSet<>();
      for (Task task : tasks) {
        sortedTasks.addAll(generateSortedTasks(task, seenIds));
      }
      return sortedTasks.build();
    }

    /**
     * Recursively traverses the acyclic task graph depth-first such that dependencies always
     * precede their dependents.
     */
    private static List<Task> generateSortedTasks(Task task, MutableSet<Task.Id> seenIds) {
      ImmutableList.Builder<Task> taskList = ImmutableList.builder();
      if (!seenIds.contains(task.id())) {
        seenIds.add(task.id());
        for (Task dependency : task.dependencies()) {
          taskList.addAll(generateSortedTasks(dependency, seenIds));
        }
        taskList.add(task);
      }
      return taskList.build();
    }

    private static String createTaskString(Task task) {
      return new StringBuilder()
          .append(task.id().asLong())
          .append(';')
          .append(escapist().escape(task.label()))
          .append(';')
          .append(
              task.dependencies()
                  .stream()
                  .map(Task::id)
                  .map(Task.Id::asLong)
                  .map(l -> Long.toString(l))
                  .collect(Collectors.joining(",")))
          .append(';')
          .append(Boolean.toString(task.isCompleted()))
          .toString();
    }
  }
}

