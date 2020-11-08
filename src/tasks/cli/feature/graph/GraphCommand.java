package tasks.cli.feature.graph;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.command.Command;
import tasks.cli.command.FlagOption;

public final class GraphCommand {
  private GraphCommand() {}

  public static Command registration() {
    return COMMAND.value();
  }

  public static final Memoized<FlagOption> ALL_OPTION =
      memoize(
          () -> new FlagOption(
              "all",
              "a",
              "Lists all tasks, including completed tasks. By default, completed tasks are not " +
                  "included in the graph.",
              NOT_REPEATABLE));

  private static final Memoized<Command> COMMAND =
      memoize(
          () -> Command.builder()
              .canonicalName("graph")
              .aliases("xl")
              .parameters(ImmutableList.empty())
              .options(ImmutableList.of(ALL_OPTION.value()))
              .helpDocumentation("Prints all tasks in graph format."));
}
