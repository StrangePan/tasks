package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableList;
import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import java.util.function.Function;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliUtils.ParseResult;
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

  public static abstract class Parser<T extends SimpleArguments> implements CliArguments.Parser<T> {
    private final Function<List<Task>, T> constructor;
    private final Memoized<CliArguments.Parser<? extends List<ParseResult<Task>>>> taskParser;

    protected Parser(
        Memoized<CliArguments.Parser<? extends List<ParseResult<Task>>>> taskParser,
        Function<List<Task>, T> constructor) {
      this.constructor = requireNonNull(constructor);
      this.taskParser = requireNonNull(taskParser);
    }

    @Override
    public T parse(List<? extends String> args) {
      /*
      1st param assumed to be "remove" or an alias for it.
      2nd+ params must be task IDs
      */
      List<String> argsList =
          List.masking(
              tryParse(args.stream().skip(1).collect(toImmutableList()), new Options())
                  .getArgList());
      if (argsList.count() < 1) {
        throw new CliArguments.ArgumentFormatException("No task IDs specified");
      }

      List<ParseResult<Task>> parsedTasks = taskParser.value().parse(argsList);

      validateParsedTasks(parsedTasks);

      return constructor.apply(extractTasksFrom(parsedTasks));
    }
  }
}
