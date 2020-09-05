package tasks.cli.command.add;

import static omnia.data.cache.Memoized.memoize;
import static tasks.cli.command.Parameter.Repeatable.NOT_REPEATABLE;
import static tasks.cli.command.Parameter.Repeatable.REPEATABLE;

import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.parser.ParserUtil;
import tasks.cli.command.Command;
import tasks.cli.command.Option;
import tasks.cli.command.Parameter;
import tasks.cli.command.StringParameter;
import tasks.cli.command.TaskOption;
import tasks.cli.parser.Parser;
import tasks.model.Task;

/** Canonical definition for the Add command. */
public final class AddCommand {
  private AddCommand() {}

  public static Command registration(
      Memoized<Parser<? extends List<ParserUtil.ParseResult<Task>>>> taskParser) {
    return Command.builder()
        .canonicalName("add")
        .aliases()
        .parameters(COMMAND_PARAMETERS.value())
        .options(OPTIONS.value())
        .parser(() -> new AddParser(taskParser))
        .helpDocumentation("Creates a new task.");
  }

  static final Memoized<ImmutableList<Parameter>> COMMAND_PARAMETERS =
      memoize(
          () -> ImmutableList.of(new StringParameter("description", NOT_REPEATABLE)));

  static final Memoized<TaskOption> AFTER_OPTION =
      memoize(
          () -> new TaskOption(
              "after",
              "a",
              "The tasks this one comes after. Tasks listed here will be blocking this task.",
              REPEATABLE));

  static final Memoized<TaskOption> BEFORE_OPTION =
      memoize(
          () -> new TaskOption(
              "before",
              "b",
              "The tasks this one comes before. Tasks listed here will be blocked by this task.",
              REPEATABLE));

  static final Memoized<ImmutableList<Option>> OPTIONS =
      memoize(() -> ImmutableList.of(AFTER_OPTION.value(), BEFORE_OPTION.value()));
}
