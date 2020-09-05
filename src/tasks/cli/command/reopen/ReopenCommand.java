package tasks.cli.command.reopen;

import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.parser.ParserUtil;
import tasks.cli.command.Command;
import tasks.cli.command.TaskParameter;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Canonical definition for the Reopen command. */
public final class ReopenCommand {
  private ReopenCommand() {}

  public static Command registration(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    return Command.builder()
        .canonicalName("reopen")
        .aliases()
        .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new ReopenParser(taskParser))
        .helpDocumentation(
            "Reopens one or more completed tasks. This can be undone with the complete "
                + "command.");
  }
}
