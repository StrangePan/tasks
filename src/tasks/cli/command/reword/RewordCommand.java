package tasks.cli.command.reword;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.parser.ParserUtil;
import tasks.cli.command.Command;
import tasks.cli.command.Parameter;
import tasks.cli.command.StringParameter;
import tasks.cli.command.TaskParameter;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Canonical definition for the Reword command. */
public final class RewordCommand {
  private RewordCommand() {}

  public static Command registration(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    return Command.builder()
        .canonicalName("reword")
        .aliases()
        .parameters(COMMAND_PARAMETERS.value())
        .options(ImmutableList.empty())
        .parser(() -> new RewordParser(taskParser))
        .helpDocumentation("Changes the description of a task.");
  }

  static final Memoized<ImmutableList<Parameter>> COMMAND_PARAMETERS =
      memoize(
          () ->
              ImmutableList.of(
                  new TaskParameter(NOT_REPEATABLE),
                  new StringParameter("description", NOT_REPEATABLE)));
}
