package tasks.cli.command.info;

import omnia.data.structure.List;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

public final class InfoArguments extends SimpleArguments {
  InfoArguments(List<Task> tasks) {
    super(tasks);
  }
}
