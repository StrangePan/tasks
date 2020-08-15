package tasks.cli.command.add;

import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.model.Task;

public final class AddParser implements CliArguments.Parser<AddArguments> {
  private final Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser;

  public AddParser(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    this.taskParser = taskParser;
  }

  @Override
  public AddArguments parse(List<? extends String> args) {
    /*
    1st param assumed to be "add" or an alias for it
    2nd param must be description
    3+ params not supported
    optional befores
    optional afters
    */
    Options options = CliUtils.toOptions(AddCommand.OPTIONS.value());

    CommandLine commandLine = tryParse(args, options);

    List<String> argsList = List.masking(commandLine.getArgList());
    String taskDescription = extractTaskDescriptionFrom(argsList)
        .orElseThrow(
            () -> new CliArguments.ArgumentFormatException("Task description not defined"));
    CliUtils.assertNoExtraArgs(commandLine, AddCommand.COMMAND_PARAMETERS.value());

    List<CliUtils.ParseResult<Task>> afterTasks = taskParser.value().parse(getOptionValues(commandLine, AddCommand.AFTER_OPTION.value()));
    List<CliUtils.ParseResult<Task>> beforeTasks = taskParser.value().parse(getOptionValues(commandLine, AddCommand.BEFORE_OPTION.value()));

    validateParsedTasks(
        ImmutableList.<CliUtils.ParseResult<?>>builder().addAll(afterTasks).addAll(beforeTasks).build());

    return new AddArguments(
        taskDescription, extractTasksFrom(afterTasks), extractTasksFrom(beforeTasks));
  }

  private static Optional<String> extractTaskDescriptionFrom(List<String> args) {
    return args.count() < 2 ? Optional.empty() : Optional.of(args.itemAt(1));
  }
}
