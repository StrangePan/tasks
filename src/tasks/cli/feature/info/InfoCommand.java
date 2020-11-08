package tasks.cli.feature.info;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.TaskParameter;

/** Canonical definition for the Info command. */
public final class InfoCommand {
  private InfoCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("info")
              .aliases("i")
              .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
              .options(ImmutableList.empty())
              .helpDocumentation(
                  "Prints all known information about a particular task, including its "
                      + "description, all tasks blocking it, and all tasks it is blocking."));
}
