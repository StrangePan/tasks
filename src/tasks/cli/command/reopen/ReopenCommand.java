package tasks.cli.command.reopen;

import static tasks.cli.arg.registration.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.registration.CommandRegistration;
import tasks.cli.arg.registration.TaskParameter;
import tasks.model.Task;

/** Canonical definition for the Reopen command. */
public final class ReopenCommand {
  private ReopenCommand() {}

  public static CommandRegistration registration(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CommandRegistration.builder()
        .canonicalName("reopen")
        .aliases()
        .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new ReopenParser(taskParser))
        .helpDocumentation(
            "Reopens one or more completed tasks. This can be undone with the complete "
                + "command.");
  }
}
