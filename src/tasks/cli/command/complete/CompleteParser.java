package tasks.cli.command.complete;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

/** Command line argument parser for the Complete command. */
public final class CompleteParser extends SimpleArguments.Parser<CompleteArguments> {
  public CompleteParser(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    super(taskParser, CompleteArguments::new);
  }
}
