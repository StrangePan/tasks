package tasks.cli.feature.reword;

import static tasks.cli.parser.ParserUtil.extractSuccessfulResultOrThrow;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.ParserException;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.ParserUtil;
import tasks.cli.parser.CommandParser;
import tasks.cli.parser.Parser;
import tasks.model.ObservableTask;

/** Command line argument parser for the Reword command. */
public final class  RewordParser implements CommandParser<RewordArguments> {
  private final Memoized<? extends Parser<? extends List<? extends ParseResult<? extends ObservableTask>>>>
      taskParser;

  public RewordParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends ObservableTask>>>>
          taskParser) {
    this.taskParser = taskParser;
  }

  @Override
  public RewordArguments parse(CommandLine commandLine) {
    /*
     * 1st param must be task id
     * 2nd param must be description
     * 3+ params not supported
     */
    List<String> argsList = ImmutableList.copyOf(commandLine.getArgList());

    if (argsList.count() < 1) {
      throw new ParserException("No task ID or description specified");
    }
    if (argsList.count() < 2) {
      throw new ParserException("No description specified");
    }
    ParserUtil.assertNoExtraArgs(commandLine, RewordCommand.COMMAND_PARAMETERS.value());


    ParseResult<? extends ObservableTask> targetTask =
        taskParser.value().parse(ImmutableList.of(argsList.itemAt(0))).itemAt(0);
    String description = argsList.itemAt(1);

    return new RewordArguments(extractSuccessfulResultOrThrow(targetTask), description);
  }
}
