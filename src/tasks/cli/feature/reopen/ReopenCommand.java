package tasks.cli.feature.reopen;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.TaskParameter;

/** Canonical definition for the Reopen command. */
public final class ReopenCommand {
  private ReopenCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("reopen")
              .aliases()
              .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
              .options(ImmutableList.empty())
              .helpDocumentation(
                  "Reopens one or more completed tasks. This can be undone with the complete "
                      + "command."));
}
