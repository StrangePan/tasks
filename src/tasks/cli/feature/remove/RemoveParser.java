package tasks.cli.feature.remove;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Remove command. */
public final class RemoveParser extends SimpleParser<RemoveArguments> {
  public RemoveParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
    super(RemoveArguments::new, taskParser);
  }
}
