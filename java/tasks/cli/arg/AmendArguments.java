package tasks.cli.arg;

import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.getSingleOptionValue;
import static tasks.cli.arg.CliUtils.parseTaskIds;
import static tasks.cli.arg.CliUtils.tryParse;

import java.util.Optional;
import omnia.data.structure.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import tasks.Task;

public final class AmendArguments {
  private final Task.Id targetTask;
  private final Optional<String> description;
  private final List<Task.Id> blockingTasks;
  private final List<Task.Id> blockingTasksToAdd;
  private final List<Task.Id> blockingTasksToRemove;
  private final List<Task.Id> blockedTasks;
  private final List<Task.Id> blockedTasksToAdd;
  private final List<Task.Id> blockedTasksToRemove;

  private AmendArguments(
      Task.Id targetTask,
      Optional<String> description,
      List<Task.Id> blockingTasks,
      List<Task.Id> blockingTasksToAdd,
      List<Task.Id> blockingTasksToRemove,
      List<Task.Id> blockedTasks,
      List<Task.Id> blockedTasksToAdd,
      List<Task.Id> blockedTasksToRemove) {
    this.targetTask = targetTask;
    this.description = description;
    this.blockingTasks = blockingTasks;
    this.blockingTasksToAdd = blockingTasksToAdd;
    this.blockingTasksToRemove = blockingTasksToRemove;
    this.blockedTasks = blockedTasks;
    this.blockedTasksToAdd = blockedTasksToAdd;
    this.blockedTasksToRemove = blockedTasksToRemove;
  }

  public Task.Id targetTask() {
    return targetTask;
  }

  public Optional<String> description() {
    return description;
  }

  public List<Task.Id> blockingTasks() {
    return blockingTasks;
  }

  public List<Task.Id> blockingTasksToAdd() {
    return blockingTasksToAdd;
  }

  public List<Task.Id> blockingTasksToRemove() {
    return blockingTasksToRemove;
  }

  public List<Task.Id> blockedTasks() {
    return blockedTasks;
  }

  public List<Task.Id> blockedTasksToAdd() {
    return blockedTasksToAdd;
  }

  public List<Task.Id> blockedTasksToRemove() {
    return blockedTasksToRemove;
  }

  static AmendArguments parse(String[] args) {
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

    Task.Id targetTask = Task.Id.parse(argsList.itemAt(1));

    Optional<String> description1 = getSingleOptionValue(commandLine, "m");
    List<Task.Id> afterTasks = parseTaskIds(getOptionValues(commandLine, "a"));
    List<Task.Id> afterTasksToAdd = parseTaskIds(getOptionValues(commandLine, "aa"));
    List<Task.Id> afterTasksToRemove = parseTaskIds(getOptionValues(commandLine, "ra"));
    List<Task.Id> beforeTasks = parseTaskIds(getOptionValues(commandLine, "b"));
    List<Task.Id> beforeTasksToAdd = parseTaskIds(getOptionValues(commandLine, "ab"));
    List<Task.Id> beforeTasksToRemove = parseTaskIds(getOptionValues(commandLine, "rb"));

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

    if (!description1.isPresent()
        && !afterTasks.isPopulated()
        && !afterTasksToAdd.isPopulated()
        && !afterTasksToRemove.isPopulated()
        && !beforeTasks.isPopulated()
        && !beforeTasksToAdd.isPopulated()
        && !beforeTasksToRemove.isPopulated()) {
      throw new CliArguments.ArgumentFormatException("Nothing to amend");
    }

    return new AmendArguments(
        targetTask,
        description1,
        afterTasks,
        afterTasksToAdd,
        afterTasksToRemove,
        beforeTasks,
        beforeTasksToAdd,
        beforeTasksToRemove);
  }
}
