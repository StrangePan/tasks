package tasks.cli.command.remove;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.TaskParameter;

/** Canonical definition for the Remove command. */
public final class RemoveCommand {
  private RemoveCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("remove")
              .aliases("rm")
              .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
              .options(ImmutableList.empty())
              .helpDocumentation(
                  "Completely deletes a task. THIS CANNOT BE UNDONE. It is recommended that "
                      + "tasks be marked as completed rather than deleted, or amended if their "
                      + "content needs to change."));
}
