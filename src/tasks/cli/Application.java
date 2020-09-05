package tasks.cli;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;

import io.reactivex.Single;
import omnia.data.cache.Memoized;
import omnia.data.structure.List;
import omnia.data.structure.immutable.ImmutableList;
import tasks.cli.bindings.Feature;
import tasks.cli.bindings.Features;
import tasks.model.impl.TaskStoreImpl;

final class Application {
  private final Memoized<TaskStoreImpl> taskStore = memoize(() -> new TaskStoreImpl("asdf"));
  private final Memoized<Features> features = memoize(() -> new Features(taskStore));

  private final String[] rawArgs;

  Application(String[] rawArgs) {
    this.rawArgs = requireNonNull(rawArgs);
  }

  void run() {
    Single.just(rawArgs)
        .map(ImmutableList::copyOf)
        .flatMapCompletable(args -> getHelpOrFallbackFeature(args).handle(args))
        .blockingAwait();
  }

  private Feature<?> getHelpOrFallbackFeature(List<? extends String> args) {
    return args.stream()
        .findFirst()
        .<Feature<?>>map(arg -> features.value().getMatchingOrFallbackFeature(arg))
        .orElseGet(() -> features.value().getFallbackFeature());
  }
}
