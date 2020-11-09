package tasks.cli.feature.info;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleParser;
import tasks.cli.parser.ParseResult;
import tasks.cli.parser.Parser;
import tasks.model.ObservableTask;

/** Command line argument parser for the Info command. */
public final class InfoParser extends SimpleParser<InfoArguments> {
  public InfoParser(
      Memoized<? extends Parser<? extends List<? extends ParseResult<? extends ObservableTask>>>>
          taskParser) {
    super(InfoArguments::new, taskParser);
  }
}
