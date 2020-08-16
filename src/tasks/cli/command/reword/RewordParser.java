package tasks.cli.command.reword;

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

/** Command line argument parser for the Reword command. */
public final class RewordParser implements CliArguments.Parser<RewordArguments> {
  private final Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>>
      taskParser;

  public RewordParser(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    this.taskParser = taskParser;
  }

  @Override
  public RewordArguments parse(List<? extends String> args) {
    /*
    1st param assumed to be "reword" or an alias for it
    2nd param must be task id
    3rd param must be description
    4+ params not supported
    */
    CommandLine commandLine = tryParse(args, new Options());
    List<String> argsList = List.masking(commandLine.getArgList());

    if (argsList.count() < 2) {
      throw new CliArguments.ArgumentFormatException("No task ID or description specified");
    }
    if (argsList.count() < 3) {
      throw new CliArguments.ArgumentFormatException("No description specified");
    }
    CliUtils.assertNoExtraArgs(commandLine, RewordCommand.COMMAND_PARAMETERS.value());


    CliUtils.ParseResult<Task> targetTask =
        taskParser.value().parse(ImmutableList.of(argsList.itemAt(1))).itemAt(0);
    String description = argsList.itemAt(2);

    validateParsedTasks(ImmutableList.of(targetTask));

    return new RewordArguments(targetTask.successResult().get(), description);
  }
}
