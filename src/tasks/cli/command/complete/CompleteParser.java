package tasks.cli.command.complete;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.parser.ParserUtil;
import tasks.cli.arguments.SimpleArguments;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Complete command. */
public final class CompleteParser extends SimpleArguments.Parser<CompleteArguments> {
  public CompleteParser(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    super(taskParser, CompleteArguments::new);
  }
}
