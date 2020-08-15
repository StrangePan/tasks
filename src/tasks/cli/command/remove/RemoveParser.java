package tasks.cli.command.remove;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

public final class RemoveParser extends SimpleArguments.Parser<RemoveArguments> {
  public RemoveParser(Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    super(taskParser, RemoveArguments::new);
  }
}
