package tasks.cli.arg;

import omnia.data.structure.List;
import tasks.Task;

public final class InfoArguments extends SimpleArguments {
  private InfoArguments(List<Task.Id> taskIds) {
    super(taskIds);
  }

  static InfoArguments parse(String[] args) {
    return SimpleArguments.parse(args, InfoArguments::new);
  }
}
