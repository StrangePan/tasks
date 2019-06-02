package tasks.cli;

import omnia.data.structure.List;
import tasks.Task;

public final class ReopenArguments extends SimpleArguments {
  private ReopenArguments(List<Task.Id> tasks) {
    super(tasks);
  }

  static ReopenArguments parse(String[] args) {
    return SimpleArguments.parse(args, ReopenArguments::new);
  }
}
