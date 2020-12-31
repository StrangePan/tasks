package tasks.cli.feature.start;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.TaskParameter;

/** Canonical definition for the Start command. */
public final class StartCommand {
  private StartCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("start")
              .aliases()
              .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
              .options(ImmutableList.empty())
              .helpDocumentation(
                  "Mark one or more tasks as started. This can be undone with the stop "
                      + "command. Started tasks will be highlighted and will appear at the top of "
                      + "the list command. Starting a completed task will reopen the task, "
                      + "which may cause other tasks to become blocked."));
}
