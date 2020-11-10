package tasks.cli.feature;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableList;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.function.Supplier;
import omnia.algorithm.ListAlgorithms;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import tasks.cli.command.Command;
import tasks.cli.command.Option;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.command.common.CommonOptions;
import tasks.cli.command.common.CommonParser;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.parser.ParserException;
import tasks.cli.parser.CommandParser;
import tasks.model.ObservableTaskStore;
import tasks.model.impl.CyclicalDependencyException;

public final class Feature<T> {
  private final Command command;
  private final Supplier<? extends CommandParser<? extends T>> parser;
  private final Supplier<? extends ArgumentHandler<? super T>> handler;

  Feature(
      Command command,
      Supplier<? extends CommandParser<? extends T>> parser,
      Supplier<? extends ArgumentHandler<? super T>> handler) {
    this.command = requireNonNull(command);
    this.parser = requireNonNull(parser);
    this.handler = requireNonNull(handler);
  }

  Command command() {
    return command;
  }

  CommandParser<? extends T> obtainParser() {
    return requireNonNull(parser.get());
  }

  CommonParser obtainCommonParser() {
    return new CommonParser();
  }

  ArgumentHandler<? super T> obtainHandler() {
    return requireNonNull(handler.get());
  }


  public Completable handle(
      List<? extends String> args,
      Memoized<? extends ObservableTaskStore> taskStore) {
    return Single.just(args)
        .to(this::toArguments)
        .flatMap(this::toOutput)
        .ignoreElement()
        .andThen(taskStore.value().shutdown())
        .to(Feature::maybeConsumeError);
  }

  private Single<CommonArguments<? extends T>> toArguments(
      Single<? extends List<? extends String>> args) {
    return args
        .map(Feature::dropFirstValue)
        .map(this::toArgsAndCommand)
        .flatMap(this::toCommandLine)
        .map(
            commandLine ->
                obtainCommonParser().parse(commandLine, obtainParser().parse(commandLine)));
  }

  private static <E> ImmutableList<E> dropFirstValue(List<? extends E> list) {
    return list.stream().skip(1).collect(toImmutableList());
  }

  private Couple<List<? extends String>, Command> toArgsAndCommand(List<? extends String> args) {
    return Tuple.of(args, command());
  }

  private Single<CommandLine> toCommandLine(
      Couple<? extends List<? extends String>, Command> couple) {
    return Observable.concat(
            Single.just(couple)
                .map(Couple::second)
                .map(Command::options)
                .flatMapObservable(Observable::fromIterable),
            Single.fromCallable(CommonOptions.OPTIONS::value)
                .flatMapObservable(Observable::fromIterable))
        .<Single<Options>>to(Feature::toOptions)
        .map(options -> tryParse(couple.first(), options));
  }

  private static Single<Options> toOptions(Observable<? extends Option> options) {
    return options.map(Option::toCliOption).collect(Options::new, Options::addOption);
  }

  private static CommandLine tryParse(List<? extends String> args, Options options) {
    try {
      return new DefaultParser()
          .parse(
              options, ListAlgorithms.toArray(args, new String[0]), /* stopAtNonOption= */ false);
    } catch (ParseException e) {
      throw new ParserException("Unable to parse arguments: " + e.getMessage(), e);
    }
  }

  private Single<Output> toOutput(CommonArguments<? extends T> arguments) {
    return obtainHandler()
        .handle(arguments.specificArguments())
        .doOnSuccess(
            output ->
                System.out.print(
                    arguments.enableOutputFormatting()
                        ? output.render()
                        : output.renderWithoutCodes()));
  }

  private static Completable maybeConsumeError(Completable completable) {
    return completable
        .doOnError(Feature::maybeConsumeError)
        .onErrorComplete(Feature::maybeSuppressError);
  }

  private static void maybeConsumeError(Throwable throwable) {
    if (shouldSuppressError(throwable)) {
      System.out.println(throwable.getMessage());
    }
  }

  private static boolean maybeSuppressError(Throwable throwable) {
    return shouldSuppressError(throwable);
  }

  private static boolean shouldSuppressError(Throwable throwable) {
    for (Class<? extends Throwable> suppressedClass : SUPPRESSED_HANDLER_THROWABLE_CLASSES) {
      if (suppressedClass.isInstance(throwable)) {
        return true;
      }
    }
    return false;
  }

  // TODO: don't hard-code throwable classes.
  // We need a better way of converting an exception into a proper, human-readable output.
  private static final Set<Class<? extends Throwable>> SUPPRESSED_HANDLER_THROWABLE_CLASSES =
      ImmutableSet.of(ParserException.class, CyclicalDependencyException.class);
}
