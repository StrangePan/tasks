package tasks.cli.command.complete;

import static tasks.cli.arg.registration.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.registration.CommandRegistration;
import tasks.cli.arg.registration.TaskParameter;
import tasks.model.Task;

/** Canonical definition for the Complete command. */
public final class CompleteCommand {
  private CompleteCommand() {}

  public static CommandRegistration registration(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CommandRegistration.builder()
        .canonicalName("complete")
        .aliases()
        .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new CompleteParser(taskParser))
        .helpDocumentation(
            "Mark one or more tasks as complete. This can be undone with the reopen "
                + "command. When a task is completed, other tasks it was blocking may "
                + "become unblocked.");
  }
}
