package tasks.cli.command.reopen;

import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.model.Task;

public class ReopenCommand {
  private ReopenCommand() {}

  public static CliArguments.CommandRegistration registration(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.REOPEN)
        .canonicalName("reopen")
        .aliases()
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new ReopenParser(taskParser))
        .helpDocumentation(
            "Reopens one or more completed tasks. This can be undone with the complete "
                + "command.");
  }
}
