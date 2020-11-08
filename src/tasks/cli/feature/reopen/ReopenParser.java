package tasks.cli.feature.reopen;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Reopen command. */
public final class ReopenParser extends SimpleParser<ReopenArguments> {
  public ReopenParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
    super(ReopenArguments::new, taskParser);
  }
}
