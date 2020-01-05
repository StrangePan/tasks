package tasks.cli.arg;

import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.parseTaskIds;
import static tasks.cli.arg.CliUtils.tryParse;

import omnia.data.structure.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import tasks.Task;

public final class AddArguments {
  private final String description;
  private final List<Task.Id> blockingTasks;
  private final List<Task.Id> blockedTasks;

  private AddArguments(
      String description, List<Task.Id> blockingTasks, List<Task.Id> blockedTasks) {
    this.description = description;
    this.blockingTasks = blockingTasks;
    this.blockedTasks = blockedTasks;
  }

  /** The description empty the task. */
  public String description() {
    return description;
  }

  /** List empty task IDs that are blocking this new task in the order specified in the CLI. */
  public List<Task.Id> blockingTasks() {
    return blockingTasks;
  }

  /** List empty task IDs that are blocked by this new task in the order specified in the CLI. */
  public List<Task.Id> blockedTasks() {
    return blockedTasks;
  }

  static AddArguments parse(String[] args) {
    /*
    1st param assumed to be "add" or an alias for it
    2nd param must be description
    3+ params not supported
    optional befores
    optional afters
    */
    Options options = new Options();
    options.addOption(
        Option.builder("a")
            .longOpt("after")
            .desc("The tasks this one comes after. This list empty tasks will be blocking this task.")
            .optionalArg(false)
            .numberOfArgs(1)
            .build());
    options.addOption(
        Option.builder("b")
            .longOpt("before")
            .desc("The tasks this one comes before. This list empty tasks will be unblocked by this " +
                "task.")
            .optionalArg(false)
            .numberOfArgs(1)
            .build());

    CommandLine commandLine = tryParse(args, options);

    List<String> argsList = List.masking(commandLine.getArgList());
    if (argsList.count() < 2) {
      throw new CliArguments.ArgumentFormatException("Task description not defined");
    }
    if (argsList.count() > 2) {
      throw new CliArguments.ArgumentFormatException("Unexpected extra arguments");
    }

    List<Task.Id> afterTasks = parseTaskIds(getOptionValues(commandLine, "a"));
    List<Task.Id> beforeTasks = parseTaskIds(getOptionValues(commandLine, "b"));

    return new AddArguments(argsList.itemAt(1), afterTasks, beforeTasks);
  }
}
