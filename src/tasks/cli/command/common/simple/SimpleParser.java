package tasks.cli.command.common.simple;

import static java.util.Objects.requireNonNull;
import static tasks.cli.parser.ParserUtil.extractSuccessfulResults;
import static tasks.cli.parser.ParserUtil.validateParsedTasks;

import java.util.function.Function;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.ArgumentFormatException;
import tasks.cli.parser.CommandParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.model.Task;

public abstract class SimpleParser<T extends SimpleArguments> implements CommandParser<T> {
  private final Function<List<Task>, T> constructor;
  private final Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
      taskParser;

  protected SimpleParser(
      Function<List<Task>, T> constructor,
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
    this.constructor = requireNonNull(constructor);
    this.taskParser = requireNonNull(taskParser);
  }

  @Override
  public T parse(CommandLine commandLine) {
    /*
     * All params must be task IDs
     */
    List<String> argsList = ImmutableList.copyOf(commandLine.getArgList());
    if (argsList.count() < 1) {
      throw new ArgumentFormatException("No task IDs specified");
    }

    List<? extends ParseResult<? extends Task>> parsedTasks = taskParser.value().parse(argsList);

    validateParsedTasks(parsedTasks);

    return constructor.apply(extractSuccessfulResults(parsedTasks));
  }
}
