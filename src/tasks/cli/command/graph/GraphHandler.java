package tasks.cli.command.graph;

import static java.util.Objects.requireNonNull;

import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import tasks.cli.handler.ArgumentHandler;
import tasks.model.TaskStore;

/** Business logic for the Graph command. */
public final class GraphHandler implements ArgumentHandler<GraphArguments> {
  private final Memoized<? extends TaskStore> taskStore;

  public GraphHandler(Memoized<? extends TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(GraphArguments arguments) {
    return null;
  }
}
