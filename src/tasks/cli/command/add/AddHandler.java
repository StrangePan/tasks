package tasks.cli.command.add;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.HandlerException;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.TaskStore;

/** Business logic for the Add command. */
public final class AddHandler implements ArgumentHandler<AddArguments> {
  private final Memoized<TaskStore> taskStore;

  public AddHandler(Memoized<TaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Completable handle(AddArguments arguments) {
    // Validate arguments
    String description = arguments.description().trim();
    if (description.isEmpty()) {
      throw new HandlerException("description cannot be empty or whitespace only");
    }

    // Collect the dependencies and dependents
    Set<Task> blockingTasks = ImmutableSet.copyOf(arguments.blockingTasks());
    Set<Task> blockedTasks = ImmutableSet.copyOf(arguments.blockedTasks());

    TaskStore taskStore = this.taskStore.value();

    // Construct the new task, commit to disk, print output
    return taskStore.createTask(
        description,
        builder ->
            Single.just(builder)
                .flatMap(b ->
                    Observable.fromIterable(blockingTasks).reduce(b, TaskBuilder::addBlockingTask))
                .flatMap(b ->
                    Observable.fromIterable(blockedTasks).reduce(b, TaskBuilder::addBlockedTask))
                .blockingGet())
        .flatMap(task -> taskStore.writeToDisk().toSingleDefault(task))
        .map(Task::render)
        .map(Output::render)
        .map(taskString -> "task created: " + taskString)
        .flatMapCompletable(msg -> Completable.fromAction(() -> System.out.println(msg)));
  }
}
