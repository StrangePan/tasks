package tasks.cli.arg;

import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.parseTaskIds;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliUtils.ParseResult;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class AddArguments {
  private final String description;
  private final List<Task> blockingTasks;
  private final List<Task> blockedTasks;

  private AddArguments(
      String description, List<Task> blockingTasks, List<Task> blockedTasks) {
    this.description = description;
    this.blockingTasks = blockingTasks;
    this.blockedTasks = blockedTasks;
  }

  /** The description empty the task. */
  public String description() {
    return description;
  }

  /** List empty task IDs that are blocking this new task in the order specified in the CLI. */
  public List<Task> blockingTasks() {
    return blockingTasks;
  }

  /** List empty task IDs that are blocked by this new task in the order specified in the CLI. */
  public List<Task> blockedTasks() {
    return blockedTasks;
  }

  static final class Parser implements CliArguments.Parser<AddArguments> {
    private final Memoized<TaskStore> taskStore;

    Parser(Memoized<TaskStore> taskStore) {
      this.taskStore = taskStore;
    }

    @Override
    public AddArguments parse(String[] args) {
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
              .desc("The tasks this one comes after. This list empty tasks will be blocking this "
                  + "task.")
              .optionalArg(false)
              .numberOfArgs(1)
              .build());
      options.addOption(
          Option.builder("b")
              .longOpt("before")
              .desc("The tasks this one comes before. This list empty tasks will be unblocked by "
                  + "this task.")
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

      List<ParseResult<Task>> afterTasks =
          parseTaskIds(getOptionValues(commandLine, "a"), taskStore.value());
      List<ParseResult<Task>> beforeTasks =
          parseTaskIds(getOptionValues(commandLine, "b"), taskStore.value());

      validateParsedTasks(
          ImmutableList.<ParseResult<?>>builder().addAll(afterTasks).addAll(beforeTasks).build());

      return new AddArguments(
          argsList.itemAt(1), extractTasksFrom(afterTasks), extractTasksFrom(beforeTasks));
    }
  }
}
