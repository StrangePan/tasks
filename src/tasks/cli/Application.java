package tasks.cli;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.RegisteredParsers;
import tasks.cli.parser.ArgumentFormatException;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.arg.RegisteredHandlers;
import tasks.model.TaskStore;
import tasks.model.impl.CyclicalDependencyException;
import tasks.model.impl.TaskStoreImpl;

final class Application {
  private final String[] rawArgs;

  private final Memoized<TaskStore> taskStore = memoize(() -> new TaskStoreImpl("asdf"));

  private final Memoized<RegisteredParsers> argumentsParser;
  private final Memoized<ArgumentHandler<CommonArguments<?>>> argumentHandler;

  Application(String[] rawArgs) {
    this.rawArgs = requireNonNull(rawArgs);

    argumentsParser = memoize(() -> new RegisteredParsers(taskStore));
    argumentHandler = memoize(() ->
        RegisteredHandlers.create(
            taskStore, memoize(() -> argumentsParser.value().commandDocumentation())));
  }

  void run() {
    Single.just(rawArgs)
        .flatMapMaybe(this::parseCliArguments)
        .flatMapCompletable(this::handleCliArguments)
        .blockingAwait();
  }


  private Maybe<CommonArguments<?>> parseCliArguments(String[] args) {
    return Single.just(args)
        .map(ImmutableList::copyOf)
        .<CommonArguments<?>>map(a -> argumentsParser.value().parse(a))
        .toMaybe()
        .doOnError(Application::handleParseError)
        .onErrorComplete();
  }

  private static void handleParseError(Throwable throwable) {
    if (throwable instanceof ArgumentFormatException) {
      System.out.println(throwable.getMessage());
    } else {
      throwable.printStackTrace();
    }
  }

  private Completable handleCliArguments(CommonArguments<?> args) {
    return Single.just(args)
        .flatMap(arguments -> argumentHandler.value().handle(arguments))
        .doOnSuccess(
            output ->
                System.out.print(
                    args.enableOutputFormatting() ? output.render() : output.renderWithoutCodes()))
        .ignoreElement()
        .doOnError(Application::maybeHandleError)
        .onErrorComplete(Application::maybeConsumeError);
  }

  private static void maybeHandleError(Throwable throwable) {
    if (shouldSuppressThrowable(throwable)) {
      System.out.println(throwable.getMessage());
    }
  }

  private static boolean maybeConsumeError(Throwable throwable) {
    return shouldSuppressThrowable(throwable);
  }

  private static boolean shouldSuppressThrowable(Throwable throwable) {
    for (Class<? extends Throwable> suppressedClass : suppressedThrowableClasses) {
      if (suppressedClass.isInstance(throwable)) {
        return true;
      }
    }
    return false;
  }

  // TODO: don't hard-code a CyclicalDependencyException directly in Application.
  // We need a better way of converting an exception into a proper, human-readable output.
  private static final Set<Class<? extends Throwable>> suppressedThrowableClasses =
      ImmutableSet.of(CyclicalDependencyException.class);
}
