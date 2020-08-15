package tasks.cli.command.amend;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;
import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.getSingleOptionValue;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import java.util.Optional;
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

public final class AmendArguments {
  private final Task targetTask;
  private final Optional<String> description;
  private final List<Task> blockingTasks;
  private final List<Task> blockingTasksToAdd;
  private final List<Task> blockingTasksToRemove;
  private final List<Task> blockedTasks;
  private final List<Task> blockedTasksToAdd;
  private final List<Task> blockedTasksToRemove;

  private static final Memoized<CliArguments.StringOption> DESCRIPTION_OPTION =
      memoize(
          () -> new CliArguments.StringOption(
              "description",
              "m",
              "Set the task description.",
              NOT_REPEATABLE,
              "description"));

  private static final Memoized<CliArguments.TaskOption> AFTER_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "after",
              "a",
              "Sets this task as coming after another task. Tasks listed here will "
                  + "be blocking this task. Removes all previous blocking tasks.",
              REPEATABLE));

  private static final Memoized<CliArguments.TaskOption> ADD_AFTER_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "addafter",
              "aa",
              "Adds another task as blocking this one.",
              REPEATABLE));

  private static final Memoized<CliArguments.TaskOption> REMOVE_AFTER_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "rmafter",
              "ra",
              "Removes another task as blocking this one.",
              REPEATABLE));

  private static final Memoized<CliArguments.TaskOption> BEFORE_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "before",
              "b",
              "Sets this task as coming before another task. Tasks listed here will "
                  + "be blocked by this task. Removes all previous blocked tasks.",
              REPEATABLE));

  private static final Memoized<CliArguments.TaskOption> ADD_BEFORE_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "addbefore",
              "ab",
              "Adds another task as being blocked by this one.",
              REPEATABLE));

  private static final Memoized<CliArguments.TaskOption> REMOVE_BEFORE_OPTION =
      memoize(
          () -> new CliArguments.TaskOption(
              "rmbefore",
              "rb",
              "Removes another task as being blocked by this one.",
              REPEATABLE));

  private static final Memoized<ImmutableList<CliArguments.Option>> OPTIONS =
      memoize(
          () -> ImmutableList.of(
              DESCRIPTION_OPTION.value(),
              AFTER_OPTION.value(),
              ADD_AFTER_OPTION.value(),
              REMOVE_AFTER_OPTION.value(),
              BEFORE_OPTION.value(),
              ADD_BEFORE_OPTION.value(),
              REMOVE_BEFORE_OPTION.value()));

  public static CliArguments.CommandRegistration registration(Memoized<CliArguments.Parser<? extends List<ParseResult<Task>>>> taskParser) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.AMEND)
        .canonicalName("amend")
        .aliases()
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(NOT_REPEATABLE)))
        .options(OPTIONS.value())
        .parser(() -> new AmendArguments.Parser(taskParser))
        .helpDocumentation(
            "Changes an existing task. Can be used to change the task description or to "
                + "add/remove blocking/blocked tasks.");
  }

  private AmendArguments(
      Task targetTask,
      Optional<String> description,
      List<Task> blockingTasks,
      List<Task> blockingTasksToAdd,
      List<Task> blockingTasksToRemove,
      List<Task> blockedTasks,
      List<Task> blockedTasksToAdd,
      List<Task> blockedTasksToRemove) {
    this.targetTask = targetTask;
    this.description = description;
    this.blockingTasks = blockingTasks;
    this.blockingTasksToAdd = blockingTasksToAdd;
    this.blockingTasksToRemove = blockingTasksToRemove;
    this.blockedTasks = blockedTasks;
    this.blockedTasksToAdd = blockedTasksToAdd;
    this.blockedTasksToRemove = blockedTasksToRemove;
  }

  public Task targetTask() {
    return targetTask;
  }

  public Optional<String> description() {
    return description;
  }

  public List<Task> blockingTasks() {
    return blockingTasks;
  }

  public List<Task> blockingTasksToAdd() {
    return blockingTasksToAdd;
  }

  public List<Task> blockingTasksToRemove() {
    return blockingTasksToRemove;
  }

  public List<Task> blockedTasks() {
    return blockedTasks;
  }

  public List<Task> blockedTasksToAdd() {
    return blockedTasksToAdd;
  }

  public List<Task> blockedTasksToRemove() {
    return blockedTasksToRemove;
  }

  public final static class Parser implements CliArguments.Parser<AmendArguments> {
    private final Memoized<CliArguments.Parser<? extends List<ParseResult<Task>>>> taskParser;

    public Parser(Memoized<CliArguments.Parser<? extends List<ParseResult<Task>>>> taskParser) {
        this.taskParser = requireNonNull(taskParser);
    }

    @Override
    public AmendArguments parse(List<? extends String> args) {
      /*
      1st param assumed to be "amend" or an alias for it
      2nd param must a task ID
      3+ params not supported
      optional description replacement
      optional after=, after+=, and after-=. after= cannot be used with after+= and after-=.
      optional before=, before+=, and before-=. before= cannot be used with before+= and before-=.
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

      ParseResult<Task> targetTask = taskParser.value().parse(ImmutableList.of(argsList.itemAt(1))).itemAt(0);

      Optional<String> description1 = getSingleOptionValue(commandLine, DESCRIPTION_OPTION.value());
      List<ParseResult<Task>> afterTasks = taskParser.value().parse(getOptionValues(commandLine, AFTER_OPTION.value()));
      List<ParseResult<Task>> afterTasksToAdd = taskParser.value().parse(getOptionValues(commandLine, ADD_AFTER_OPTION.value()));
      List<ParseResult<Task>> afterTasksToRemove = taskParser.value().parse(getOptionValues(commandLine, REMOVE_AFTER_OPTION.value()));
      List<ParseResult<Task>> beforeTasks = taskParser.value().parse(getOptionValues(commandLine, BEFORE_OPTION.value()));
      List<ParseResult<Task>> beforeTasksToAdd = taskParser.value().parse(getOptionValues(commandLine, ADD_BEFORE_OPTION.value()));
      List<ParseResult<Task>> beforeTasksToRemove = taskParser.value().parse(getOptionValues(commandLine, REMOVE_BEFORE_OPTION.value()));

      if (afterTasks.isPopulated()
          && (afterTasksToAdd.isPopulated() || afterTasksToRemove.isPopulated())) {
        throwOptionsMustBeMutuallyExclusive(AFTER_OPTION, ADD_AFTER_OPTION, REMOVE_AFTER_OPTION);
      }

      if (beforeTasks.isPopulated()
          && (beforeTasksToAdd.isPopulated() || beforeTasksToRemove.isPopulated())) {
        throwOptionsMustBeMutuallyExclusive(BEFORE_OPTION, ADD_BEFORE_OPTION, REMOVE_BEFORE_OPTION);
      }

      if (description1.isEmpty()
          && !afterTasks.isPopulated()
          && !afterTasksToAdd.isPopulated()
          && !afterTasksToRemove.isPopulated()
          && !beforeTasks.isPopulated()
          && !beforeTasksToAdd.isPopulated()
          && !beforeTasksToRemove.isPopulated()) {
        throw new CliArguments.ArgumentFormatException("Nothing to amend");
      }

      validateParsedTasks(
          ImmutableList.<ParseResult<?>>builder()
              .add(targetTask)
              .addAll(afterTasks)
              .addAll(afterTasksToAdd)
              .addAll(afterTasksToRemove)
              .addAll(beforeTasks)
              .addAll(beforeTasksToAdd)
              .addAll(beforeTasksToRemove)
              .build());

      return new AmendArguments(
          targetTask.successResult().get(),
          description1,
          extractTasksFrom(afterTasks),
          extractTasksFrom(afterTasksToAdd),
          extractTasksFrom(afterTasksToRemove),
          extractTasksFrom(beforeTasks),
          extractTasksFrom(beforeTasksToAdd),
          extractTasksFrom(beforeTasksToRemove));
    }
  }

  private static void throwOptionsMustBeMutuallyExclusive(
      Memoized<? extends CliArguments.Option> first,
      Memoized<? extends CliArguments.Option> second,
      Memoized<? extends CliArguments.Option> third) {
    throw new CliArguments.ArgumentFormatException(
        String.format(
            "--%s cannot be used with --%s or --%s",
            first.value().longName(),
            second.value().longName(),
            third.value().longName()));

  }
}
