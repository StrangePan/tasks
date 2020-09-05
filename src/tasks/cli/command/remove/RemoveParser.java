package tasks.cli.command.remove;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.parser.ParserUtil;
import tasks.cli.arguments.SimpleArguments;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Remove command. */
public final class RemoveParser extends SimpleArguments.Parser<RemoveArguments> {
  public RemoveParser(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    super(taskParser, RemoveArguments::new);
  }
}
