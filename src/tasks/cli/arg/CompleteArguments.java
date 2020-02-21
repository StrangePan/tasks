package tasks.cli.arg;

import omnia.data.structure.List;
import tasks.cli.CliTaskId;

public final class CompleteArguments extends SimpleArguments {
  private CompleteArguments(List<CliTaskId> tasks) {
    super(tasks);
  }

  static CompleteArguments parse(String[] args) {
    return SimpleArguments.parse(args, CompleteArguments::new);
  }
}
