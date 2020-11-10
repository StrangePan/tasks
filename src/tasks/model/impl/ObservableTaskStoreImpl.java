package tasks.model.impl;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableList;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.CompletableSubject;
import java.util.Optional;
import java.util.function.Function;
import omnia.algorithm.GraphAlgorithms;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.DirectedGraph.DirectedNode;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.observable.ObservableState;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Triple;
import omnia.data.structure.tuple.Tuple;
import tasks.io.File;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.TaskMutator;
import tasks.model.ObservableTaskStore;

/** This is currently NOT thread-safe. */
public final class ObservableTaskStoreImpl implements ObservableTaskStore {

  private final CompletableSubject isShutdown = CompletableSubject.create();
  private final Completable isShutdownComplete;

  private final TaskFileSource fileSource;

  private final ObservableState<TaskStoreImpl> store;
  private final Observable<Observable<TaskStoreImpl>> currentStoreSource;

  public ObservableTaskStoreImpl(String filePath) {
    this.fileSource = new TaskFileSource(File.fromPath(filePath));
    Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>> loadedData =
        fileSource.readFromFile().blockingGet();

    store =
        ObservableState.create(
            new TaskStoreImpl(
                loadedData.first(),
                loadedData.second()));

    currentStoreSource =
        selectWhileRunning(
            store.observe(),
            new TaskStoreImpl(ImmutableDirectedGraph.empty(), ImmutableMap.empty()));

    isShutdownComplete = isShutdown.hide().andThen(writeToDisk()).cache();
  }

  private <T> Observable<Observable<T>> selectWhileRunning(
      Observable<? extends T> starter, T finisher) {
    return Observable.just(starter.map(i -> (T) i))
        .concatWith(isShutdown.hide().andThen(Observable.just(Observable.just(finisher))))
        .replay(1)
        .autoConnect(0);
  }

  @Override
  public Observable<TaskStoreImpl> observe() {
    return currentStoreSource.switchMap(store1 -> store1);
  }

  @Override
  public Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> createTask(
      String label, Function<? super TaskBuilder, ? extends TaskBuilder> builder) {
    return Single.just(new TaskBuilderImpl(this, label))
        .<TaskBuilder>map(builder::apply)
        .flatMap(this::maybeApplyBuilder)
        .cache();
  }

  /**
   * Attempts to apply the given {@link TaskBuilder} to the task graph, but in a manner that tries
   * to preserve internal consistency by first applying the changes to a snapshot and validating
   * the snapshot.
   */
  private Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> maybeApplyBuilder(
      TaskBuilder builder) {
    TaskBuilderImpl builderImpl = validateBuilder(builder);

    return store.mutateAndReturn(taskStore -> applyBuilderTo(builderImpl, taskStore))
        .map(Couple::second);
  }

  private static Couple<TaskStoreImpl, Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>>
      applyBuilderTo(TaskBuilderImpl builder, TaskStoreImpl oldStore) {
    TaskIdImpl id = TaskIdImpl.generate(oldStore.graph.contents());

    ImmutableDirectedGraph.Builder<TaskIdImpl> newGraphBuilder =
        oldStore.graph.toBuilder().addNode(id);
    builder.blockingTasks().forEach(blockingTask -> newGraphBuilder.addEdge(blockingTask, id));
    builder.blockedTasks().forEach(blockedTask -> newGraphBuilder.addEdge(id, blockedTask));

    ImmutableDirectedGraph<TaskIdImpl> newGraph = newGraphBuilder.build();
    ImmutableMap<TaskIdImpl, TaskData> newData =
        oldStore.data.toBuilder()
            .putMapping(id, new TaskData(builder.completed(), builder.label()))
            .build();

    assertIsValid(newGraph, newData);

    TaskStoreImpl newStore = new TaskStoreImpl(newGraph, newData);

    return Tuple.of(newStore, Tuple.of(oldStore, newStore, newStore.toTask(id)));
  }

  private static void assertIsValid(
      DirectedGraph<TaskIdImpl> taskGraph, Map<TaskIdImpl, TaskData> taskData) {
    GraphAlgorithms.findAnyCycle(taskGraph)
        .map(
            cycle ->
                cycle.stream()
                    .map(
                        id -> id.toString()
                            + ": "
                            + taskData.valueOf(id).map(TaskData::label).orElse(""))
                    .collect(toImmutableList()))
        .ifPresent(
            cycle -> {
              throw new CyclicalDependencyException("Cycle detected", cycle);
            });
  }

  TaskBuilderImpl validateBuilder(TaskBuilder builder) {
    requireNonNull(builder);
    if (!(builder instanceof TaskBuilderImpl)) {
      throw new IllegalArgumentException(
          "Unrecognized builder type. Expected "
              + TaskBuilderImpl.class
              + ", received "
              + builder.getClass()
              + ": "
              + builder);
    }
    TaskBuilderImpl builderImpl = (TaskBuilderImpl) builder;
    if (builderImpl.store() != this) {
      throw new IllegalArgumentException(
          "Builder associated with another store. Expected <"
              + this
              + ">, received <"
              + builderImpl.store()
              + ">: "
              + builderImpl);
    }
    return builderImpl;
  }

  @Override
  public Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> mutateTask(
      Task task, Function<? super TaskMutator, ? extends TaskMutator> mutation) {
    return mutateTask(validateTask(task), mutation);
  }

  Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> mutateTask(
      TaskImpl task, Function<? super TaskMutator, ? extends TaskMutator> mutation) {
    return Single.just(new TaskMutatorImpl(this, task.id()))
        .<TaskMutator>map(mutation::apply)
        .flatMap(this::maybeApplyMutator)
        .cache();
  }

  TaskImpl validateTask(Task task) {
    requireNonNull(task);
    if (!(task instanceof TaskImpl)) {
      throw new IllegalArgumentException(
          "Unrecognized task type. Expected "
              + TaskImpl.class
              + ", received "
              + task.getClass()
              + ": "
              + task);
    }
    return (TaskImpl) task;
  }

  private Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> maybeApplyMutator(
      TaskMutator mutator) {
    TaskMutatorImpl mutatorImpl = validateMutator(mutator);

    return store.mutateAndReturn(
        oldStore -> {
          ImmutableDirectedGraph<TaskIdImpl> nextTaskGraph =
              applyMutatorTo(oldStore.graph, mutatorImpl);
          ImmutableMap<TaskIdImpl, TaskData> nextTaskData = applyMutatorTo(oldStore.data, mutatorImpl);
          assertIsValid(nextTaskGraph, nextTaskData);
          TaskStoreImpl newStore = new TaskStoreImpl(nextTaskGraph, nextTaskData);
          return Tuple.of(
              newStore, Tuple.of(oldStore, newStore, newStore.toTask(mutatorImpl.id())));
        })
        .map(Couple::second);
  }

  TaskMutatorImpl validateMutator(TaskMutator mutator) {
    requireNonNull(mutator);
    if (!(mutator instanceof TaskMutatorImpl)) {
      throw new IllegalArgumentException(
          "Unrecognized mutator type. Expected "
              + TaskMutatorImpl.class
              + ", received "
              + mutator.getClass()
              + ": "
              + mutator);
    }
    TaskMutatorImpl mutatorImpl = (TaskMutatorImpl) mutator;
    if (mutatorImpl.store() != this) {
      throw new IllegalArgumentException(
          "Mutator associated with another store. Expected <"
              + this
              + ">, received <"
              + mutatorImpl.store()
              + ">: "
              + mutatorImpl);
    }
    return mutatorImpl;
  }

  private static ImmutableMap<TaskIdImpl, TaskData> applyMutatorTo(
      ImmutableMap<TaskIdImpl, TaskData> taskData, TaskMutatorImpl mutatorImpl) {
    return Optional.of(mutatorImpl)
        .filter(mutator -> mutator.completed().isPresent() || mutator.label().isPresent())
        .map(TaskMutatorImpl::id)
        .flatMap(taskData::valueOf)
        .map(
            data -> new TaskData(
                mutatorImpl.completed().orElse(data.isCompleted()),
                mutatorImpl.label().orElse(data.label())))
        .map(data -> taskData.toBuilder().putMapping(mutatorImpl.id(), data).build())
        .orElse(taskData);
  }

  private static ImmutableDirectedGraph<TaskIdImpl> applyMutatorTo(
      ImmutableDirectedGraph<TaskIdImpl> taskGraph, TaskMutatorImpl mutatorImpl) {
    return Optional.of(mutatorImpl)
        .filter(
            mutator -> mutator.overwriteBlockedTasks()
                || mutator.blockedTasksToAdd().isPopulated()
                || mutator.blockedTasksToRemove().isPopulated()
                || mutator.overwriteBlockingTasks()
                || mutator.blockingTasksToAdd().isPopulated()
                || mutator.blockingTasksToRemove().isPopulated())
        .map(
            mutator -> {
              TaskIdImpl id = mutator.id();
              ImmutableDirectedGraph.Builder<TaskIdImpl> builder = taskGraph.toBuilder();
              if (mutator.overwriteBlockingTasks()) {
                taskGraph.nodeOf(id)
                    .map(DirectedNode::incomingEdges)
                    .map(ImmutableSet::copyOf)
                    .orElse(ImmutableSet.empty())
                    .forEach(edge -> builder.removeEdge(edge.start(), edge.end()));
              }
              mutator.blockingTasksToAdd()
                  .forEach(blockingId -> builder.addEdge(blockingId, id));
              mutator.blockingTasksToRemove()
                  .forEach(blockingId -> builder.removeEdge(blockingId, id));

              if (mutator.overwriteBlockedTasks()) {
                taskGraph.nodeOf(id)
                    .map(DirectedNode::outgoingEdges)
                    .map(ImmutableSet::copyOf)
                    .orElse(ImmutableSet.empty())
                    .forEach(edge -> builder.removeEdge(edge.start(), edge.end()));
              }
              mutator.blockedTasksToAdd()
                  .forEach(blockedId -> builder.addEdge(id, blockedId));
              mutator.blockedTasksToRemove()
                  .forEach(blockedId -> builder.removeEdge(id, blockedId));

              return builder.build();
            })
        .orElse(taskGraph);
  }

  @Override
  public Completable deleteTask(Task task) {
    return deleteTask(validateTask(task));
  }

  private Completable deleteTask(TaskImpl task) {
    return update(
        taskStoreImpl ->
          new TaskStoreImpl(
              taskStoreImpl.graph.toBuilder().removeNode(task.id()).build(),
              taskStoreImpl.data.toBuilder().removeKey(task.id()).build()))
        .ignoreElement();
  }

  Single<TaskStoreImpl> update(Function<? super TaskStoreImpl, ? extends TaskStoreImpl> updater) {
    return store.mutate(updater::apply).cache();
  }

  @Override
  public Completable writeToDisk() {
    return store.observe()
        .firstOrError()
        .flatMapCompletable(store -> fileSource.writeToFile(store.graph, store.data))
        .cache();
  }

  @Override
  public Completable shutdown() {
    return isShutdownComplete.doOnSubscribe(d -> isShutdown.onComplete());
  }
}
