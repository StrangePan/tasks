package tasks.cli.arg;

import static omnia.data.stream.Collectors.toList;
import static tasks.cli.arg.CliUtils.parseTaskIds;
import static tasks.cli.arg.CliUtils.tryParse;

import java.util.function.Function;
import omnia.data.structure.List;
import org.apache.commons.cli.Options;
import tasks.cli.CliTaskId;

abstract class SimpleArguments {
  private final List<CliTaskId> tasks;

  protected SimpleArguments(List<CliTaskId> tasks) {
    this.tasks = tasks;
  }

  public List<CliTaskId> tasks() {
    return tasks;
  }

  static <T extends SimpleArguments> T parse(
      String[] args, Function<List<CliTaskId>, T> constructor) {
    /*
    1st param assumed to be "remove" or an alias for it.
    2nd+ params must be task IDs
    */

    List<String> argsList = List.masking(tryParse(args, new Options()).getArgList());
    if (argsList.count() < 2) {
      throw new CliArguments.ArgumentFormatException("No task IDs specified");
    }

    List<CliTaskId> taskIds = parseTaskIds(argsList.stream().skip(1).collect(toList()));

    return constructor.apply(taskIds);
  }
}
