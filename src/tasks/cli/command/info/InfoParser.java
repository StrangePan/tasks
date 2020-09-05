package tasks.cli.command.info;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Info command. */
public final class InfoParser extends SimpleParser<InfoArguments> {
  public InfoParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends Task>>>>
          taskParser) {
    super(InfoArguments::new, taskParser);
  }
}
