package tasks.cli.command.reword;

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

/** Command line argument parser for the Reword command. */
public final class  RewordParser implements CommandParser<RewordArguments> {
  private final Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>>
      taskParser;

  public RewordParser(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
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
      throw new ArgumentFormatException("No task ID or description specified");
    }
    if (argsList.count() < 2) {
      throw new ArgumentFormatException("No description specified");
    }
    ParserUtil.assertNoExtraArgs(commandLine, RewordCommand.COMMAND_PARAMETERS.value());


    ParserUtil.ParseResult<Task> targetTask =
        taskParser.value().parse(ImmutableList.of(argsList.itemAt(0))).itemAt(0);
    String description = argsList.itemAt(1);

    validateParsedTasks(ImmutableList.of(targetTask));

    return new RewordArguments(targetTask.successResult().get(), description);
  }
}
