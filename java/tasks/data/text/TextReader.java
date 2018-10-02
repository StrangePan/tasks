package tasks.data.text;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import omnia.data.structure.Collection;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import tasks.Task;
import tasks.Task.Id;
import tasks.data.TaskReader;

public final class TextReader implements TaskReader {

  private final Path path;

  public TextReader(Path path) {
    this.path = requireNonNull(path);
  }

  @Override
  public Collection<Task> read() throws IOException {
    MutableMap<Id, Task> taskMap = new HashMap<>();
    return Files.lines(path)
        .map(taskString -> parseTaskString(taskString, taskMap))
        .collect(toImmutableSet());
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
            .label(Utils.escapist().unescape(parts[1]))
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
