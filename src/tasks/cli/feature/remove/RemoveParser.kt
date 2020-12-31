package tasks.cli.feature.remove;

import static java.util.Objects.requireNonNull;
import static tasks.cli.parser.ParserUtil.extractSuccessfulResultsOrThrow;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.CommandParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.cli.parser.ParserException;
import tasks.cli.parser.ParserUtil;
import tasks.model.Task;

/** Command line argument parser for the Remove command. */
public final class RemoveParser implements CommandParser<RemoveArguments> {
  private final Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
      taskParser;

  public RemoveParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
    this.taskParser = requireNonNull(taskParser);
  }

  @Override
  public RemoveArguments parse(CommandLine commandLine) {
    /*
     * All params must be task IDs.
     * optional --force flag
     */
    List<String> argsList = ImmutableList.copyOf(commandLine.getArgList());
    if (argsList.count() < 1) {
      throw new ParserException("No task IDs specified");
    }

    return new RemoveArguments(
        extractSuccessfulResultsOrThrow(taskParser.value().parse(argsList)),
        ParserUtil.getFlagPresence(commandLine, RemoveCommand.FORCE_OPTION.value()));
  }

}
