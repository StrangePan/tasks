package tasks.cli.command.add;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;
import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.parseTaskIds;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliArguments.CommandRegistration;
import tasks.cli.arg.CliArguments.TaskOption;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.CliUtils.ParseResult;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class AddArguments {
  private final String description;
  private final List<Task> blockingTasks;
  private final List<Task> blockedTasks;

  private static final Memoized<CliArguments.StringParameter> DESCRIPTION_PARAMETER =
      memoize(() -> new CliArguments.StringParameter("description", NOT_REPEATABLE));

  private static final Memoized<ImmutableList<CliArguments.Parameter>> COMMAND_PARAMETERS =
      memoize(() -> ImmutableList.of(DESCRIPTION_PARAMETER.value()));

  private static final Memoized<CliArguments.TaskOption> AFTER_OPTION =
      memoize(
          () -> new TaskOption(
              "after",
              "a",
              "The tasks this one comes after. Tasks listed here will be blocking this task.",
              REPEATABLE));

  private static final Memoized<CliArguments.TaskOption> BEFORE_OPTION =
      memoize(
          () -> new TaskOption(
              "before",
              "b",
              "The tasks this one comes before. Tasks listed here will be blocked by this task.",
              REPEATABLE));

  private static final Memoized<ImmutableList<CliArguments.Option>> OPTIONS =
      memoize(() -> ImmutableList.of(AFTER_OPTION.value(), BEFORE_OPTION.value()));

  public static CommandRegistration registration(Memoized<TaskStore> taskStore) {
    return CommandRegistration.builder()
        .cliMode(CliMode.ADD)
        .canonicalName("add")
        .aliases()
        .parameters(COMMAND_PARAMETERS.value())
        .options(OPTIONS.value())
        .parser(() -> new AddArguments.Parser(taskStore))
        .helpDocumentation("Creates a new task.");
  }

  private AddArguments(String description, List<Task> blockingTasks, List<Task> blockedTasks) {
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

  public static final class Parser implements CliArguments.Parser<AddArguments> {
    private final Memoized<TaskStore> taskStore;

    public Parser(Memoized<TaskStore> taskStore) {
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
      Options options = CliUtils.toOptions(OPTIONS.value());

      CommandLine commandLine = tryParse(args, options);

      List<String> argsList = List.masking(commandLine.getArgList());
      String taskDescription = extractTaskDescriptionFrom(argsList)
          .orElseThrow(
              () -> new CliArguments.ArgumentFormatException("Task description not defined"));
      CliUtils.assertNoExtraArgs(commandLine, COMMAND_PARAMETERS.value());

      List<ParseResult<Task>> afterTasks =
          parseTaskIds(getOptionValues(commandLine, AFTER_OPTION.value()), taskStore.value());
      List<ParseResult<Task>> beforeTasks =
          parseTaskIds(getOptionValues(commandLine, BEFORE_OPTION.value()), taskStore.value());

      validateParsedTasks(
          ImmutableList.<ParseResult<?>>builder().addAll(afterTasks).addAll(beforeTasks).build());

      return new AddArguments(
          taskDescription, extractTasksFrom(afterTasks), extractTasksFrom(beforeTasks));
    }
  }

  private static Optional<String> extractTaskDescriptionFrom(List<String> args) {
    return args.count() < 2 ? Optional.empty() : Optional.of(args.itemAt(1));
  }
}
