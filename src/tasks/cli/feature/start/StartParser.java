package tasks.cli.feature.start;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Start command. */
public final class StartParser extends SimpleParser<StartArguments> {
  public StartParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
    super(StartArguments::new, taskParser);
  }
}
