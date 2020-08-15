package tasks.cli.command.info;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.SimpleArguments;
import tasks.model.Task;

public final class InfoParser extends SimpleArguments.Parser<InfoArguments> {
  public InfoParser(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    super(taskParser, InfoArguments::new);
  }
}
