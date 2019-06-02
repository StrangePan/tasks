package tasks.cli;

import omnia.data.structure.List;
import tasks.Task;

public final class RemoveArguments extends SimpleArguments {
  private RemoveArguments(List<Task.Id> tasks) {
    super(tasks);
  }

  static RemoveArguments parse(String[] args) {
    return SimpleArguments.parse(args, RemoveArguments::new);
  }
}
