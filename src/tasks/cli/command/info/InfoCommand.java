package tasks.cli.command.info;

import static tasks.cli.arg.registration.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliMode;
import tasks.cli.arg.CliUtils;
import tasks.cli.arg.registration.CommandRegistration;
import tasks.cli.arg.registration.TaskParameter;
import tasks.model.Task;

/** Canonical definition for the Info command. */
public final class InfoCommand {
  private InfoCommand() {}

  public static CommandRegistration registration(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    return CommandRegistration.builder()
        .cliMode(CliMode.INFO)
        .canonicalName("info")
        .aliases("i")
        .parameters(ImmutableList.of(new TaskParameter(REPEATABLE)))
        .options(ImmutableList.empty())
        .parser(() -> new InfoParser(taskParser))
        .helpDocumentation(
            "Prints all known information about a particular task, including its "
                + "description, all tasks blocking it, and all tasks it is blocking.");
  }
}
