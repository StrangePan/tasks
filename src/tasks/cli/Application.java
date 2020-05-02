package tasks.cli;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import omnia.data.cache.Memoized;
import tasks.cli.arg.CliArguments;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.ArgumentHandlers;
import tasks.model.TaskStore;
import tasks.model.impl.TaskStoreImpl;

final class Application {
  private final String[] rawArgs;

  private final Memoized<TaskStore> taskStore = Memoized.memoize(() -> new TaskStoreImpl("asdf"));

  private final Memoized<CliArguments> argumentsParser;

  private final Memoized<ArgumentHandler<Object>> argumentHandler =
      Memoized.memoize(ArgumentHandlers::create);

  Application(String[] rawArgs) {
    this.rawArgs = requireNonNull(rawArgs);

    argumentsParser = Memoized.memoize(() -> new CliArguments(taskStore));
  }

  void run() {
    Maybe.just(rawArgs)
        .compose(this::parseCliArguments)
        .compose(this::handleCliArguments)
        .flatMapCompletable(c -> c)
        .blockingAwait();
  }


  private Maybe<Object> parseCliArguments(Maybe<String[]> args) {
    return args
        .map(a -> argumentsParser.value().parse(a))
        .doOnError(e -> System.out.println(e.getMessage()))
        .onErrorComplete();
  }

  private Maybe<Completable> handleCliArguments(Maybe<Object> args) {
    return args.flatMap(arguments -> argumentHandler.value().handle(arguments).toMaybe());
  }

}
