package tasks.cli.command.blockers;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;
import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.CliUtils.ParseResult;
import tasks.model.Task;

public final class BlockersArguments {
  private final Task targetTask;
  private final List<Task> blockingTasksToAdd;
  private final List<Task> blockingTasksToRemove;
  private final boolean clearAllBlockers;

  private static final Memoized<CliArguments.TaskOption> ADD_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "add",
              "a",
              "Adds another task as a blocker.",
              REPEATABLE));

  private static final Memoized<CliArguments.FlagOption> CLEAR_OPTION =
      memoize(
          () -> new CliArguments.FlagOption(
              "clear",
              "c",
              "Removes all blocking tasks. Can be used together with --"
                  + ADD_OPTION.value().longName() + " to replace existing blockers with new ones.",
              NOT_REPEATABLE));

  private static final Memoized<CliArguments.TaskOption> REMOVE_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "remove",
              "d",
              "Removes another task from being a blocker.",
              REPEATABLE));

  private static final Memoized<ImmutableList<CliArguments.Option>> OPTIONS =
      memoize(
          () -> ImmutableList.of(
              ADD_OPTION.value(),
              CLEAR_OPTION.value(),
              REMOVE_OPTION.value()));

  public static CliArguments.CommandRegistration registration(Memoized<CliArguments.Parser<? extends List<ParseResult<Task>>>> taskParser) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.BLOCKERS)
        .canonicalName("blockers")
        .aliases("blocker", "bk")
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(NOT_REPEATABLE)))
        .options(OPTIONS.value())
        .parser(() -> new BlockersArguments.Parser(taskParser))
        .helpDocumentation(
            "Modifies or lists blockers of an existing task. Can be used to add or remove blockers "
                + "from a task. If no modifications are specified, simply lists the existing "
                + "blockers for a task.");
  }

  private BlockersArguments(
      Task targetTask,
      List<Task> blockingTasksToAdd,
      List<Task> blockingTasksToRemove,
      boolean clearAllBlockers) {
    this.targetTask = targetTask;
    this.blockingTasksToAdd = blockingTasksToAdd;
    this.blockingTasksToRemove = blockingTasksToRemove;
    this.clearAllBlockers = clearAllBlockers;
  }

  public Task targetTask() {
    return targetTask;
  }

  public List<Task> blockingTasksToAdd() {
    return blockingTasksToAdd;
  }

  public List<Task> getBlockingTasksToRemove() {
    return blockingTasksToRemove;
  }

  public boolean clearAllBlockers() {
    return clearAllBlockers;
  }

  public final static class Parser implements CliArguments.Parser<BlockersArguments> {
    private final Memoized<CliArguments.Parser<? extends List<ParseResult<Task>>>> taskParser;

    public Parser(Memoized<CliArguments.Parser<? extends List<ParseResult<Task>>>> taskParser) {
      this.taskParser = requireNonNull(taskParser);
    }

    @Override
    public BlockersArguments parse(List<? extends String> args) {
      /*
      1st param assumed to be "blockers"
      2nd param assumed to be task ID
      3+ params are unsupported
      optional add
      optional remove
      optional clear
      */
      Options options = CliUtils.toOptions(OPTIONS.value());

      CommandLine commandLine = tryParse(args, options);

      List<String> argsList = List.masking(commandLine.getArgList());
      if (argsList.count() < 2) {
        throw new CliArguments.ArgumentFormatException("Task not specified");
      }
      if (argsList.count() > 2) {
        throw new CliArguments.ArgumentFormatException("Unexpected extra arguments");
      }

      ParseResult<Task> targetTask =
          taskParser.value().parse(ImmutableList.of(argsList.itemAt(1))).itemAt(0);
      List<ParseResult<Task>> tasksToAdd =
          taskParser.value().parse(getOptionValues(commandLine, ADD_OPTION.value()));
      List<ParseResult<Task>> tasksToRemove =
          taskParser.value().parse(getOptionValues(commandLine, REMOVE_OPTION.value()));
      boolean isClearSet = commandLine.hasOption(CLEAR_OPTION.value().shortName());

      validateParsedTasks(
          ImmutableList.<ParseResult<?>>builder()
              .add(targetTask)
              .addAll(tasksToAdd)
              .addAll(tasksToRemove)
              .build());

      return new BlockersArguments(
          targetTask.successResult().get(),
          extractTasksFrom(tasksToAdd),
          extractTasksFrom(tasksToRemove),
          isClearSet);
    }
  }
}
