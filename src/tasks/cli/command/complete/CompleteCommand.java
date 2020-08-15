package tasks.cli.command.complete;

import static tasks.cli.arg.CliArguments.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.model.Task;

public class CompleteCommand {
  private CompleteCommand() {}

  public static CliArguments.CommandRegistration registration(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CliArguments.CommandRegistration.builder()
        .cliMode(CliMode.COMPLETE)
        .canonicalName("complete")
        .aliases()
        .parameters(ImmutableList.of(new CliArguments.TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new CompleteParser(taskParser))
        .helpDocumentation(
            "Mark one or more tasks as complete. This can be undone with the reopen "
                + "command. When a task is completed, other tasks it was blocking may "
                + "become unblocked.");
  }
}
