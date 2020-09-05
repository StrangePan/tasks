package tasks.cli.command.info;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.parser.ParserUtil;
import tasks.cli.arguments.SimpleArguments;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Command line argument parser for the Info command. */
public final class InfoParser extends SimpleArguments.Parser<InfoArguments> {
  public InfoParser(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    super(taskParser, InfoArguments::new);
  }
}
