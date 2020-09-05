package tasks.cli.command.info;

import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.parser.ParserUtil;
import tasks.cli.command.Command;
import tasks.cli.command.TaskParameter;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Canonical definition for the Info command. */
public final class InfoCommand {
  private InfoCommand() {}

  public static Command registration(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    return Command.builder()
        .canonicalName("info")
        .aliases("i")
        .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new InfoParser(taskParser))
        .helpDocumentation(
            "Prints all known information about a particular task, including its "
                + "description, all tasks blocking it, and all tasks it is blocking.");
  }
}
