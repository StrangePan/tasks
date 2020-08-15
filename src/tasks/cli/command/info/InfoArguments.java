package tasks.cli.command.info;

import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

public final class InfoArguments extends SimpleArguments {

  public static CliArguments.CommandRegistration registration(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.INFO)
        .canonicalName("info")
        .aliases("i")
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new InfoArguments.Parser(taskParser))
        .helpDocumentation(
            "Prints all known information about a particular task, including its "
                + "description, all tasks blocking it, and all tasks it is blocking.");
  }

  private InfoArguments(List<Task> tasks) {
    super(tasks);
  }

  public static final class Parser extends SimpleArguments.Parser<InfoArguments> {
    public Parser(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
      super(taskParser, InfoArguments::new);
    }
  }
}
