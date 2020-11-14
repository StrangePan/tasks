package tasks.cli.feature.remove;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.FlagOption;
import tasks.cli.command.TaskParameter;

/** Canonical definition for the Remove command. */
public final class RemoveCommand {
  private RemoveCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  static final Memoized<FlagOption> FORCE_OPTION =
      memoize(
          () -> new FlagOption(
              "force",
              "f",
              "Force. Automatically confirm all deletions, skipping confirmations.",
              NOT_REPEATABLE));

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("remove")
              .aliases("rm")
              .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
              .options(ImmutableList.of(FORCE_OPTION.value()))
              .helpDocumentation(
                  "Completely deletes a task. THIS CANNOT BE UNDONE. It is recommended that "
                      + "tasks be marked as completed rather than deleted, or amended if their "
                      + "content needs to change."));
}
