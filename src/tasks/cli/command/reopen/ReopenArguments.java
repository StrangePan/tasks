package tasks.cli.command.reopen;

import omnia.data.structure.List;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

public final class ReopenArguments extends SimpleArguments {
  protected ReopenArguments(List<Task> tasks) {
    super(tasks);
  }
}
