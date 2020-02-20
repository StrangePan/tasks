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
import tasks.cli.CliTaskId;

public final class AmendArguments {
  private final CliTaskId targetTask;
  private final Optional<String> description;
  private final List<CliTaskId> blockingTasks;
  private final List<CliTaskId> blockingTasksToAdd;
  private final List<CliTaskId> blockingTasksToRemove;
  private final List<CliTaskId> blockedTasks;
  private final List<CliTaskId> blockedTasksToAdd;
  private final List<CliTaskId> blockedTasksToRemove;

  private AmendArguments(
      CliTaskId targetTask,
      Optional<String> description,
      List<CliTaskId> blockingTasks,
      List<CliTaskId> blockingTasksToAdd,
      List<CliTaskId> blockingTasksToRemove,
      List<CliTaskId> blockedTasks,
      List<CliTaskId> blockedTasksToAdd,
      List<CliTaskId> blockedTasksToRemove) {
    this.targetTask = targetTask;
    this.description = description;
    this.blockingTasks = blockingTasks;
    this.blockingTasksToAdd = blockingTasksToAdd;
    this.blockingTasksToRemove = blockingTasksToRemove;
    this.blockedTasks = blockedTasks;
    this.blockedTasksToAdd = blockedTasksToAdd;
    this.blockedTasksToRemove = blockedTasksToRemove;
  }

  public CliTaskId targetTask() {
    return targetTask;
  }

  public Optional<String> description() {
    return description;
  }

  public List<CliTaskId> blockingTasks() {
    return blockingTasks;
  }

  public List<CliTaskId> blockingTasksToAdd() {
    return blockingTasksToAdd;
  }

  public List<CliTaskId> blockingTasksToRemove() {
    return blockingTasksToRemove;
  }

  public List<CliTaskId> blockedTasks() {
    return blockedTasks;
  }

  public List<CliTaskId> blockedTasksToAdd() {
    return blockedTasksToAdd;
  }

  public List<CliTaskId> blockedTasksToRemove() {
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

    CliTaskId targetTask = CliTaskId.parse(argsList.itemAt(1));

    Optional<String> description1 = getSingleOptionValue(commandLine, "m");
    List<CliTaskId> afterTasks = parseTaskIds(getOptionValues(commandLine, "a"));
    List<CliTaskId> afterTasksToAdd = parseTaskIds(getOptionValues(commandLine, "aa"));
    List<CliTaskId> afterTasksToRemove = parseTaskIds(getOptionValues(commandLine, "ra"));
    List<CliTaskId> beforeTasks = parseTaskIds(getOptionValues(commandLine, "b"));
    List<CliTaskId> beforeTasksToAdd = parseTaskIds(getOptionValues(commandLine, "ab"));
    List<CliTaskId> beforeTasksToRemove = parseTaskIds(getOptionValues(commandLine, "rb"));

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
