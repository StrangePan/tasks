package tasks.cli.command.graph;

import static omnia.data.cache.Memoized.memoize;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.FlagOption;
import tasks.cli.command.list.ListCommand;

public final class GraphCommand {
  private GraphCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  static final Memoized<FlagOption> ALL_OPTION = ListCommand.ALL_OPTION;

  static final Memoized<FlagOption> COMPLETED_OPTION = ListCommand.COMPLETED_OPTION;

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("graph")
              .aliases("xl")
              .parameters(ListCommand.registration().parameters())
              .options(ImmutableList.of(COMPLETED_OPTION.value(), ALL_OPTION.value()))
              .helpDocumentation(
                  "Prints a list of tasks in a graph format. By default, only lists uncompleted "
                      + "tasks. Various flags are available to list only completed tasks or all "
                      + "tasks."));
}
