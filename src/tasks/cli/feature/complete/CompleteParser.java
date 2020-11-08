package tasks.cli.feature.complete;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Complete command. */
public final class CompleteParser extends SimpleParser<CompleteArguments> {
  public CompleteParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
    super(CompleteArguments::new, taskParser);
  }
}
