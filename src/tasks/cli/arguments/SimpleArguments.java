package tasks.cli.arguments;

import static java.util.Objects.requireNonNull;
import static tasks.cli.parser.ParserUtil.extractTasksFrom;
import static tasks.cli.parser.ParserUtil.validateParsedTasks;

import java.util.function.Function;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import tasks.cli.parser.ArgumentFormatException;
import tasks.cli.parser.ParserUtil.ParseResult;
import tasks.cli.parser.CommandParser;
import tasks.model.Task;

public abstract class SimpleArguments {
  private final List<Task> tasks;

  protected SimpleArguments(List<Task> tasks) {
    this.tasks = tasks;
  }

  /** The list of tasks parsed from the command line. */
  protected List<Task> tasks() {
    return tasks;
  }

  public static abstract class Parser<T extends SimpleArguments> implements CommandParser<T> {
    private final Function<List<Task>, T> constructor;
    private final Memoized<tasks.cli.parser.Parser<? extends List<ParseResult<Task>>>> taskParser;

    protected Parser(
        Memoized<tasks.cli.parser.Parser<? extends List<ParseResult<Task>>>> taskParser,
        Function<List<Task>, T> constructor) {
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

      List<ParseResult<Task>> parsedTasks = taskParser.value().parse(argsList);

      validateParsedTasks(parsedTasks);

      return constructor.apply(extractTasksFrom(parsedTasks));
    }
  }
}
