package tasks.cli;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.arg.CliArguments;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.ArgumentHandlers;
import tasks.model.TaskStore;
import tasks.model.impl.TaskStoreImpl;

final class Application {
  private final String[] rawArgs;

  private final Memoized<TaskStore> taskStore = memoize(() -> new TaskStoreImpl("asdf"));

  private final Memoized<CliArguments> argumentsParser;
  private final Memoized<ArgumentHandler<Object>> argumentHandler;

  Application(String[] rawArgs) {
    this.rawArgs = requireNonNull(rawArgs);

    argumentsParser = memoize(() -> new CliArguments(taskStore));
    argumentHandler = memoize(() ->
        ArgumentHandlers.create(
            taskStore, memoize(argumentsParser.value()::commandDocumentation)));
  }

  void run() {
    Maybe.just(rawArgs)
        .compose(this::parseCliArguments)
        .compose(this::handleCliArguments)
        .flatMapCompletable(c -> c)
        .blockingAwait();
  }


  private Maybe<Object> parseCliArguments(Maybe<?  extends String[]> args) {
    return args
        .map(ImmutableList::copyOf)
        .map(a -> argumentsParser.value().parse(a))
        .doOnError(Application::handleParseError)
        .onErrorComplete();
  }

  private static void handleParseError(Throwable throwable) {
    if (throwable instanceof CliArguments.ArgumentFormatException) {
      System.out.println(throwable.getMessage());
    } else {
        throwable.printStackTrace();
    }
  }

  private Maybe<Completable> handleCliArguments(Maybe<Object> args) {
    return args.flatMap(arguments -> argumentHandler.value().handle(arguments).toMaybe());
  }

}
