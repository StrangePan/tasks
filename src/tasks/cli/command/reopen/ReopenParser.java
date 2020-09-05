package tasks.cli.command.reopen;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.parser.ParserUtil;
import tasks.cli.arguments.SimpleArguments;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Reopen command. */
public final class ReopenParser extends SimpleArguments.Parser<ReopenArguments> {
  public ReopenParser(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    super(taskParser, ReopenArguments::new);
  }
}
