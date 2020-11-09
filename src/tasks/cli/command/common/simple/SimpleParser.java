package tasks.cli.command.common.simple;

import static java.util.Objects.requireNonNull;
import static tasks.cli.parser.ParserUtil.extractSuccessfulResultsOrThrow;

import java.util.function.Function;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.ParserException;
import tasks.cli.parser.CommandParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.model.ObservableTask;

public abstract class SimpleParser<T extends SimpleArguments> implements CommandParser<T> {
  private final Function<List<ObservableTask>, T> constructor;
  private final Memoized<? extends Parser<? extends List<? extends ParseResult<? extends ObservableTask>>>>
      taskParser;

  protected SimpleParser(
      Function<List<ObservableTask>, T> constructor,
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends ObservableTask>>>>
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
      throw new ParserException("No task IDs specified");
    }

    return constructor.apply(
        extractSuccessfulResultsOrThrow(taskParser.value().parse(argsList)));
  }
}
