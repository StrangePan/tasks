package tasks.cli.arg;

import omnia.data.structure.List;
import tasks.cli.CliTaskId;

public final class RemoveArguments extends SimpleArguments {
  private RemoveArguments(List<CliTaskId> tasks) {
    super(tasks);
  }

  static RemoveArguments parse(String[] args) {
    return SimpleArguments.parse(args, RemoveArguments::new);
  }
}
