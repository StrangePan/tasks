package tasks.cli.command.remove;

import static tasks.cli.arg.registration.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.registration.CommandRegistration;
import tasks.cli.arg.registration.TaskParameter;
import tasks.model.Task;

/** Canonical definition for the Remove command. */
public final class RemoveCommand {
  private RemoveCommand() {}

  public static CommandRegistration registration(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CommandRegistration.builder()
        .canonicalName("remove")
        .aliases("rm")
        .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new RemoveParser(taskParser))
        .helpDocumentation(
            "Completely deletes a task. THIS CANNOT BE UNDONE. It is recommended that "
                + "tasks be marked as completed rather than deleted, or amended if their "
                + "content needs to change.");
  }
}
