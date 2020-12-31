package tasks.cli.feature.blockers;

import static java.util.Objects.requireNonNull;
import static tasks.cli.parser.ParserUtil.extractSuccessfulResultOrThrow;
import static tasks.cli.parser.ParserUtil.getFlagPresence;
import static tasks.cli.parser.ParserUtil.getOptionValues;
import static tasks.cli.parser.ParserUtil.extractSuccessfulResultsOrThrow;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.ParserException;
import tasks.cli.parser.CommandParser;
import tasks.cli.parser.Parser;
import tasks.cli.parser.ParseResult;
import tasks.model.Task;

/** Command line argument parser for the Blockers command. */
public final class BlockersParser implements CommandParser<BlockersArguments> {
  private final Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
      taskParser;

  public BlockersParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
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
      throw new ParserException("ObservableTask not specified");
    }
    if (argsList.count() > 1) {
      throw new ParserException("Unexpected extra arguments");
    }

    ParseResult<? extends Task> targetTask =
        taskParser.value().parse(
            ImmutableList.of(argsList.itemAt(0))).itemAt(0);
    List<? extends ParseResult<? extends Task>> tasksToAdd =
        taskParser.value().parse(
            getOptionValues(commandLine, BlockersCommand.ADD_OPTION.value()));
    List<? extends ParseResult<? extends Task>> tasksToRemove =
        taskParser.value().parse(
            getOptionValues(commandLine, BlockersCommand.REMOVE_OPTION.value()));
    boolean isClearSet =
        getFlagPresence(commandLine, BlockersCommand.CLEAR_OPTION.value());

    extractSuccessfulResultsOrThrow(
        ImmutableList.<ParseResult<?>>builder()
            .add(targetTask)
            .addAll(tasksToAdd)
            .addAll(tasksToRemove)
            .build());

    return new BlockersArguments(
        extractSuccessfulResultOrThrow(targetTask),
        extractSuccessfulResultsOrThrow(tasksToAdd),
        extractSuccessfulResultsOrThrow(tasksToRemove),
        isClearSet);
  }
}
