package tasks.cli.handlers;

import io.reactivex.Observable;
import io.reactivex.Single;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.cli.CliTaskId;
import tasks.cli.arg.AddArguments;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.TaskStore;

public final class AddHandler implements ArgumentHandler<AddArguments> {

  @Override
  public void handle(AddArguments arguments) {
    // Validate arguments
    String label = arguments.description().trim();
    if (label.isEmpty()) {
      throw new HandlerException("description cannot be empty or whitespace only");
    }

    TaskStore taskStore = HandlerUtil.loadTaskStore();
    HandlerUtil.validateTasksIds(
        taskStore,
        ImmutableList.<CliTaskId>builder()
            .addAll(arguments.blockingTasks())
            .addAll(arguments.blockedTasks())
            .build());

    // Collect the dependencies and dependents
    Set<Task> blockingTasks =
        ImmutableSet.copyOf(
            Observable.fromIterable(arguments.blockingTasks())
                .map(CliTaskId::asLong)
                .flatMapMaybe(taskStore::lookUpById)
                .blockingIterable());
    Set<Task> blockedTasks =
        ImmutableSet.copyOf(
            Observable.fromIterable(arguments.blockedTasks())
                .map(CliTaskId::asLong)
                .flatMapMaybe(taskStore::lookUpById)
                .blockingIterable());

    // Construct the new task
    taskStore.createTask(label, builder ->
        Single.just(builder)
          .flatMap(b ->
              Observable.fromIterable(blockingTasks).reduce(b, TaskBuilder::addBlockingTask))
          .flatMap(b ->
              Observable.fromIterable(blockedTasks).reduce(b, TaskBuilder::addBlockedTask))
        .blockingGet());

    // Construct a new task graph with the new task inserted and the new edges assembled
    taskStore.writeToDisk().blockingAwait();
  }
}
