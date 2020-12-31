package tasks.cli.feature.stop;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.TaskParameter;

/** Canonical definition for the Stop command. */
public final class StopCommand {
  private StopCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("stop")
              .aliases()
              .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
              .options(ImmutableList.empty())
              .helpDocumentation(
                  "Mark one or more tasks as open. This is the opposite of the start command. " +
                      "Only tasks started with the start command can be stopped."));
}
