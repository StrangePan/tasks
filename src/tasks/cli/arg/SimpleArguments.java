package tasks.cli.arg;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toList;
import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.parseTaskIds;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import java.util.function.Function;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliUtils.ParseResult;
import tasks.model.Task;
import tasks.model.TaskStore;

abstract class SimpleArguments {
  private final List<Task> tasks;

  protected SimpleArguments(List<Task> tasks) {
    this.tasks = tasks;
  }

  public List<Task> tasks() {
    return tasks;
  }

  static abstract class Parser<T extends SimpleArguments> implements CliArguments.Parser<T> {
    private final Function<List<Task>, T> constructor;
    private final Memoized<TaskStore> taskStore;

    protected Parser(Memoized<TaskStore> taskStore, Function<List<Task>, T> constructor) {
      this.constructor = requireNonNull(constructor);
      this.taskStore = requireNonNull(taskStore);
    }

    @Override
    public T parse(String[] args) {
      /*
      1st param assumed to be "remove" or an alias for it.
      2nd+ params must be task IDs
      */
      List<String> argsList = List.masking(tryParse(args, new Options()).getArgList());
      if (argsList.count() < 2) {
        throw new CliArguments.ArgumentFormatException("No task IDs specified");
      }

      TaskStore taskStore = this.taskStore.value();
      List<ParseResult<Task>> parsedTasks =
          parseTaskIds(argsList.stream().skip(1).collect(toList()), taskStore);

      validateParsedTasks(parsedTasks);

      return constructor.apply(extractTasksFrom(parsedTasks));
    }
  }
}
