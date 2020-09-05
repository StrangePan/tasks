package tasks.cli.command.blockers;

import static java.util.Objects.requireNonNull;
import static tasks.cli.parser.ParserUtil.extractTasksFrom;
import static tasks.cli.parser.ParserUtil.getFlagPresence;
import static tasks.cli.parser.ParserUtil.getOptionValues;
import static tasks.cli.parser.ParserUtil.validateParsedTasks;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.ArgumentFormatException;
import tasks.cli.parser.ParserUtil;
import tasks.cli.parser.CommandParser;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Blockers command. */
public final class BlockersParser implements CommandParser<BlockersArguments> {
  private final Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>>
      taskParser;

  public BlockersParser(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    this.taskParser = requireNonNull(taskParser);
  }

  @Override
  public BlockersArguments parse(CommandLine commandLine) {
    /*
     * 1st param assumed to be task ID
     * 2+ params are unsupported
     * optional blockers to add
     * optional blockers to remove
     * optional clear flag
     */
    List<String> argsList = ImmutableList.copyOf(commandLine.getArgList());
    if (argsList.count() < 1) {
      throw new ArgumentFormatException("Task not specified");
    }
    if (argsList.count() > 1) {
      throw new ArgumentFormatException("Unexpected extra arguments");
    }

    ParserUtil.ParseResult<Task> targetTask =
        taskParser.value().parse(
            ImmutableList.of(argsList.itemAt(0))).itemAt(0);
    List<ParserUtil.ParseResult<Task>> tasksToAdd =
        taskParser.value().parse(
            getOptionValues(commandLine, BlockersCommand.ADD_OPTION.value()));
    List<ParserUtil.ParseResult<Task>> tasksToRemove =
        taskParser.value().parse(
            getOptionValues(commandLine, BlockersCommand.REMOVE_OPTION.value()));
    boolean isClearSet =
        getFlagPresence(commandLine, BlockersCommand.CLEAR_OPTION.value());

    validateParsedTasks(
        ImmutableList.<ParserUtil.ParseResult<?>>builder()
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
