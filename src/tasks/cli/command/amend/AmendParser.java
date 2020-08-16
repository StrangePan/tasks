package tasks.cli.command.amend;

import static java.util.Objects.requireNonNull;
import static tasks.cli.arg.CliUtils.assertNoExtraArgs;
import static tasks.cli.arg.CliUtils.extractTasksFrom;
import static tasks.cli.arg.CliUtils.getOptionValues;
import static tasks.cli.arg.CliUtils.getSingleOptionValue;
import static tasks.cli.arg.CliUtils.tryParse;
import static tasks.cli.arg.CliUtils.validateParsedTasks;

import java.util.Optional;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CliUtils;
import tasks.model.Task;

public final class AmendParser implements CliArguments.Parser<AmendArguments> {
  private final Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>>
      taskParser;

  public AmendParser(
      Memoized<CliArguments.Parser<? extends List<CliUtils.ParseResult<Task>>>> taskParser) {
    this.taskParser = requireNonNull(taskParser);
  }

  @Override
  public AmendArguments parse(List<? extends String> args) {
    /*
    1st param assumed to be "amend" or an alias for it
    2nd param must a task ID
    3+ params not supported
    optional description replacement
    optional after=, after+=, and after-=. after= cannot be used with after+= and after-=.
    optional before=, before+=, and before-=. before= cannot be used with before+= and before-=.
    */
    Options options = CliUtils.toOptions(AmendCommand.OPTIONS.value());

    CommandLine commandLine = tryParse(args, options);

    List<String> argsList = List.masking(commandLine.getArgList());
    if (argsList.count() < 2) {
      throw new CliArguments.ArgumentFormatException("Task not specified");
    }
    assertNoExtraArgs(commandLine, AmendCommand.PARAMETERS.value());

    CliUtils.ParseResult<Task> targetTask =
        taskParser.value().parse(ImmutableList.of(argsList.itemAt(1))).itemAt(0);

    Optional<String> description =
        getSingleOptionValue(commandLine, AmendCommand.DESCRIPTION_OPTION.value());
    List<CliUtils.ParseResult<Task>> afterTasks =
        taskParser.value().parse(
            getOptionValues(commandLine, AmendCommand.AFTER_OPTION.value()));
    List<CliUtils.ParseResult<Task>> afterTasksToAdd =
        taskParser.value().parse(
            getOptionValues(commandLine, AmendCommand.ADD_AFTER_OPTION.value()));
    List<CliUtils.ParseResult<Task>> afterTasksToRemove =
        taskParser.value().parse(
            getOptionValues(commandLine, AmendCommand.REMOVE_AFTER_OPTION.value()));
    List<CliUtils.ParseResult<Task>> beforeTasks =
        taskParser.value().parse(
            getOptionValues(commandLine, AmendCommand.BEFORE_OPTION.value()));
    List<CliUtils.ParseResult<Task>> beforeTasksToAdd =
        taskParser.value().parse(
            getOptionValues(commandLine, AmendCommand.ADD_BEFORE_OPTION.value()));
    List<CliUtils.ParseResult<Task>> beforeTasksToRemove =
        taskParser.value().parse(
            getOptionValues(commandLine, AmendCommand.REMOVE_BEFORE_OPTION.value()));

    if (afterTasks.isPopulated()
        && (afterTasksToAdd.isPopulated() || afterTasksToRemove.isPopulated())) {
      throwOptionsMustBeMutuallyExclusive(
          AmendCommand.AFTER_OPTION,
          AmendCommand.ADD_AFTER_OPTION,
          AmendCommand.REMOVE_AFTER_OPTION);
    }

    if (beforeTasks.isPopulated()
        && (beforeTasksToAdd.isPopulated() || beforeTasksToRemove.isPopulated())) {
      throwOptionsMustBeMutuallyExclusive(
          AmendCommand.BEFORE_OPTION,
          AmendCommand.ADD_BEFORE_OPTION,
          AmendCommand.REMOVE_BEFORE_OPTION);
    }

    if (description.isEmpty()
        && !afterTasks.isPopulated()
        && !afterTasksToAdd.isPopulated()
        && !afterTasksToRemove.isPopulated()
        && !beforeTasks.isPopulated()
        && !beforeTasksToAdd.isPopulated()
        && !beforeTasksToRemove.isPopulated()) {
      throw new CliArguments.ArgumentFormatException("Nothing to amend");
    }

    validateParsedTasks(
        ImmutableList.<CliUtils.ParseResult<?>>builder()
            .add(targetTask)
            .addAll(afterTasks)
            .addAll(afterTasksToAdd)
            .addAll(afterTasksToRemove)
            .addAll(beforeTasks)
            .addAll(beforeTasksToAdd)
            .addAll(beforeTasksToRemove)
            .build());

    return new AmendArguments(
        targetTask.successResult().get(),
        description,
        extractTasksFrom(afterTasks),
        extractTasksFrom(afterTasksToAdd),
        extractTasksFrom(afterTasksToRemove),
        extractTasksFrom(beforeTasks),
        extractTasksFrom(beforeTasksToAdd),
        extractTasksFrom(beforeTasksToRemove));
  }

  private static void throwOptionsMustBeMutuallyExclusive(
      Memoized<? extends CliArguments.Option> first,
      Memoized<? extends CliArguments.Option> second,
      Memoized<? extends CliArguments.Option> third) {
    throw new CliArguments.ArgumentFormatException(
        String.format(
            "--%s cannot be used with --%s or --%s",
            first.value().longName(),
            second.value().longName(),
            third.value().longName()));
  }
}
