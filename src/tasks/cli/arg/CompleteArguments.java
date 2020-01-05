package tasks.cli.arg;

import omnia.data.structure.List;
import tasks.Task;

public final class CompleteArguments extends SimpleArguments {
  private CompleteArguments(List<Task.Id> tasks) {
    super(tasks);
  }

  static CompleteArguments parse(String[] args) {
    return SimpleArguments.parse(args, CompleteArguments::new);
  }
}
