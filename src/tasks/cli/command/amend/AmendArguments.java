package tasks.cli.command.amend;

import static java.util.Objects.requireNonNull;
import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.getSingleOptionValue;
import static tasks.cli.arg.CliUtils.parseTaskId;
import static tasks.cli.arg.CliUtils.parseTaskIds;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils.ParseResult;
import tasks.model.Task;
import tasks.model.TaskStore;

public final class AmendArguments {
  private final Task targetTask;
  private final Optional<String> description;
  private final List<Task> blockingTasks;
  private final List<Task> blockingTasksToAdd;
  private final List<Task> blockingTasksToRemove;
  private final List<Task> blockedTasks;
  private final List<Task> blockedTasksToAdd;
  private final List<Task> blockedTasksToRemove;

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
    private final Memoized<TaskStore> taskStore;

    public Parser(Memoized<TaskStore> taskStore) {
        this.taskStore = requireNonNull(taskStore);
    }

    @Override
    public AmendArguments parse(String[] args) {
      /*
      1st param assumed to be "amend" or an alias for it
      2nd param must a task ID
      3+ params not supported
      optional description replacement
      optional after=, after+=, and after-=. after= cannot be used with after+= and after-=.
      optional before=, before+=, and before-=. before= cannot be used with before+= and before-=.
      */
      Options options = new Options();
      options.addOption(
          Option.builder("m")
              .longOpt("description")
              .desc("Edit the description empty the task. Leave blank to open in an editor")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("a")
              .longOpt("after")
              .desc("Sets this task as blocked by another task. Clears the previous blocking tasks.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("aa")
              .longOpt("addafter")
              .desc("Adds another task as blocking this one.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("ra")
              .longOpt("rmafter")
              .desc("Removes another task as blocking this one.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("b")
              .longOpt("before")
              .desc("Sets this task as blocking another task. Clears the previous blocked tasks.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("ab")
              .longOpt("addbefore")
              .desc("Adds another task as being blocked by this one.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());
      options.addOption(
          Option.builder("rb")
              .longOpt("rmbefore")
              .desc("Removes another task as being blocked by this one.")
              .numberOfArgs(1)
              .optionalArg(false)
              .build());

      CommandLine commandLine = tryParse(args, options);

      List<String> argsList = List.masking(commandLine.getArgList());
      if (argsList.count() < 2) {
        throw new CliArguments.ArgumentFormatException("Task not specified");
      }
      if (argsList.count() > 2) {
        throw new CliArguments.ArgumentFormatException("Unexpected extra arguments");
      }

      ParseResult<Task> targetTask = parseTaskId(argsList.itemAt(1), taskStore.value());

      Optional<String> description1 = getSingleOptionValue(commandLine, "m");
      List<ParseResult<Task>> afterTasks = parseTaskIds(getOptionValues(commandLine, "a"), taskStore.value());
      List<ParseResult<Task>> afterTasksToAdd = parseTaskIds(getOptionValues(commandLine, "aa"), taskStore.value());
      List<ParseResult<Task>> afterTasksToRemove = parseTaskIds(getOptionValues(commandLine, "ra"), taskStore.value());
      List<ParseResult<Task>> beforeTasks = parseTaskIds(getOptionValues(commandLine, "b"), taskStore.value());
      List<ParseResult<Task>> beforeTasksToAdd = parseTaskIds(getOptionValues(commandLine, "ab"), taskStore.value());
      List<ParseResult<Task>> beforeTasksToRemove = parseTaskIds(getOptionValues(commandLine, "rb"), taskStore.value());

      if (afterTasks.isPopulated()
          && (afterTasksToAdd.isPopulated() || afterTasksToRemove.isPopulated())) {
        throw new CliArguments.ArgumentFormatException(
            "--after cannot be use with --addafter or --rmafter");
      }

      if (beforeTasks.isPopulated()
          && (beforeTasksToAdd.isPopulated() || beforeTasksToRemove.isPopulated())) {
        throw new CliArguments.ArgumentFormatException(
            "--before cannot be use with --addbefore or --rmbefore");
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
}
