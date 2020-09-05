package tasks.cli.command.complete;

import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.parser.ParserUtil;
import tasks.cli.command.Command;
import tasks.cli.command.TaskParameter;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Canonical definition for the Complete command. */
public final class CompleteCommand {
  private CompleteCommand() {}

  public static Command registration(Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    return Command.builder()
        .canonicalName("complete")
        .aliases()
        .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new CompleteParser(taskParser))
        .helpDocumentation(
            "Mark one or more tasks as complete. This can be undone with the reopen "
                + "command. When a task is completed, other tasks it was blocking may "
                + "become unblocked.");
  }
}
