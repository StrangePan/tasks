package tasks.cli.command.reopen;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

/** Command line argument parser for the Reopen command. */
public final class ReopenParser extends SimpleArguments.Parser<ReopenArguments> {
  public ReopenParser(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    super(taskParser, ReopenArguments::new);
  }
}
