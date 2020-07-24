package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.arg.AddArguments;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.TaskStore;

public final class AddHandler implements ArgumentHandler<AddArguments> {
  private final Memoized<TaskStore> taskStore;

  AddHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(AddArguments arguments) {
    // Validate arguments
    String label = arguments.description().trim();
    if (label.isEmpty()) {
      throw new HandlerException("description cannot be empty or whitespace only");
    }

    // Collect the dependencies and dependents
    Set<Task> blockingTasks = ImmutableSet.copyOf(arguments.blockingTasks());
    Set<Task> blockedTasks = ImmutableSet.copyOf(arguments.blockedTasks());

    TaskStore taskStore = this.taskStore.value();

    // Construct the new task, commit to disk, print output
    return taskStore.createTask(
        label,
        builder ->
            Single.just(builder)
                .flatMap(b ->
                    Observable.fromIterable(blockingTasks).reduce(b, TaskBuilder::addBlockingTask))
                .flatMap(b ->
                    Observable.fromIterable(blockedTasks).reduce(b, TaskBuilder::addBlockedTask))
                .blockingGet())
        .flatMap(task -> taskStore.writeToDisk().toSingleDefault(task))
        .map(task -> "task created: " + task)
        .flatMapCompletable(msg -> Completable.fromAction(() -> System.out.println(msg)));
  }
}
