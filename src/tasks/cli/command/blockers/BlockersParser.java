package tasks.cli.command.blockers;

import static java.util.Objects.requireNonNull;
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
import tasks.cli.arg.CliUtils;
import tasks.model.Task;

public final class BlockersParser implements CliArguments.Parser<BlockersArguments> {
  private final Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser;

  public BlockersParser(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
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
    Options options = CliUtils.toOptions(BlockersCommand.OPTIONS.value());

    CommandLine commandLine = tryParse(args, options);

    List<String> argsList = List.masking(commandLine.getArgList());
    if (argsList.count() < 2) {
      throw new CliArguments.ArgumentFormatException("Task not specified");
    }
    if (argsList.count() > 2) {
      throw new CliArguments.ArgumentFormatException("Unexpected extra arguments");
    }

    CliUtils.ParseResult<Task> targetTask =
        taskParser.value().parse(ImmutableList.of(argsList.itemAt(1))).itemAt(0);
    List<CliUtils.ParseResult<Task>> tasksToAdd =
        taskParser.value().parse(getOptionValues(commandLine, BlockersCommand.ADD_OPTION.value()));
    List<CliUtils.ParseResult<Task>> tasksToRemove =
        taskParser.value().parse(getOptionValues(commandLine, BlockersCommand.REMOVE_OPTION.value()));
    boolean isClearSet = commandLine.hasOption(BlockersCommand.CLEAR_OPTION.value().shortName());

    validateParsedTasks(
        ImmutableList.<CliUtils.ParseResult<?>>builder()
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
