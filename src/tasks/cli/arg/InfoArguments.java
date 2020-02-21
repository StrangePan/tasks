package tasks.cli.arg;

import omnia.data.structure.List;
import tasks.cli.CliTaskId;

public final class InfoArguments extends SimpleArguments {
  private InfoArguments(List<CliTaskId> taskIds) {
    super(taskIds);
  }

  static InfoArguments parse(String[] args) {
    return SimpleArguments.parse(args, InfoArguments::new);
  }
}
