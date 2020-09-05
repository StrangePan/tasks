package tasks.cli.command.add;

import static tasks.cli.parser.ParserUtil.extractTasksFrom;
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
import tasks.model.Task;

/** Command line argument parser for the Add command. */
public final class AddParser implements CommandParser<AddArguments> {
  private final Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>>
      taskParser;

  public AddParser(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
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

    List<ParserUtil.ParseResult<Task>> afterTasks =
        taskParser.value().parse(getOptionValues(commandLine, AddCommand.AFTER_OPTION.value()));
    List<ParserUtil.ParseResult<Task>> beforeTasks =
        taskParser.value().parse(getOptionValues(commandLine, AddCommand.BEFORE_OPTION.value()));

    validateParsedTasks(
        ImmutableList.<ParserUtil.ParseResult<?>>builder()
            .addAll(afterTasks)
            .addAll(beforeTasks)
            .build());

    return new AddArguments(
        taskDescription, extractTasksFrom(afterTasks), extractTasksFrom(beforeTasks));
  }

  private static Optional<String> extractTaskDescriptionFrom(List<String> args) {
    return args.count() < 1 ? Optional.empty() : Optional.of(args.itemAt(0));
  }
}
