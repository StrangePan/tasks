package tasks.cli.command.complete;

import omnia.data.structure.List;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

public final class CompleteArguments extends SimpleArguments {
  CompleteArguments(List<Task> tasks) {
    super(tasks);
  }
}
