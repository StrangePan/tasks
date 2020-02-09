package tasks.cli.arg;

import omnia.data.structure.List;
import tasks.cli.CliTaskId;

public final class ReopenArguments extends SimpleArguments {
  private ReopenArguments(List<CliTaskId> tasks) {
    super(tasks);
  }

  static ReopenArguments parse(String[] args) {
    return SimpleArguments.parse(args, ReopenArguments::new);
  }
}
