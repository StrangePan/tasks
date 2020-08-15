package tasks.cli.command.remove;

import omnia.data.structure.List;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

public final class RemoveArguments extends SimpleArguments {
  RemoveArguments(List<Task> tasks) {
    super(tasks);
  }
}
