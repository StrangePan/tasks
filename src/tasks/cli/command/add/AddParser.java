package tasks.cli.command.add;

import static tasks.cli.parser.ParserUtil.extractSuccessfulResults;
import static tasks.cli.parser.ParserUtil.getOptionValues;
import static tasks.cli.parser.ParserUtil.validateParsedTasks;

import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.ArgumentFormatException;
import tasks.cli.parser.ParserUtil;
import tasks.cli.parser.CommandParser;
import tasks.cli.parser.Parser;
import tasks.cli.parser.ParseResult;
import tasks.model.Task;

/** Command line argument parser for the Add command. */
public final class AddParser implements CommandParser<AddArguments> {
  private final Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>> taskParser;

  public AddParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
    this.taskParser = taskParser;
  }

  @Override
  public AddArguments parse(CommandLine commandLine) {
    /*
     * 1st param must be description
     * 2+ params not supported
     * optional blocking tasks
     * optional blocked tasks
     */
    List<String> argsList = ImmutableList.copyOf(commandLine.getArgList());
    String taskDescription = extractTaskDescriptionFrom(argsList)
        .orElseThrow(
            () -> new ArgumentFormatException("Task description not defined"));
    ParserUtil.assertNoExtraArgs(commandLine, AddCommand.COMMAND_PARAMETERS.value());

    List<? extends ParseResult<? extends Task>> afterTasks =
        taskParser.value().parse(getOptionValues(commandLine, AddCommand.AFTER_OPTION.value()));
    List<? extends ParseResult<? extends Task>> beforeTasks =
        taskParser.value().parse(getOptionValues(commandLine, AddCommand.BEFORE_OPTION.value()));

    validateParsedTasks(
        ImmutableList.<ParseResult<?>>builder()
            .addAll(afterTasks)
            .addAll(beforeTasks)
            .build());

    return new AddArguments(
        taskDescription,
        extractSuccessfulResults(afterTasks),
        extractSuccessfulResults(beforeTasks));
  }

  private static Optional<String> extractTaskDescriptionFrom(List<String> args) {
    return args.count() < 1 ? Optional.empty() : Optional.of(args.itemAt(0));
  }
}
