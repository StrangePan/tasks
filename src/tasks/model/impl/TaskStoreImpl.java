package tasks.model.impl;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toSet;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Optional;
import java.util.function.Function;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Graph;
import omnia.data.structure.Map;
import omnia.data.structure.Pair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.observable.writable.WritableObservableDirectedGraph;
import tasks.io.Store;
import tasks.model.Task;
import tasks.model.TaskMutator;
import tasks.model.TaskStore;

final class TaskStoreImpl implements TaskStore {

  private final WritableObservableDirectedGraph<TaskId> taskGraph = WritableObservableDirectedGraph.create();
  private final MutableMap<TaskId, TaskData> taskData = HashMap.create();
  private Store<Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>>> fileStore; // TODO

  Maybe<Flowable<TaskData>> lookUp(TaskId id) {
    return Single.just(taskData.valueOf(id))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(Flowable::just);
  }

  @Override
  public Flowable<Set<Task>> allTasks() {
    return taskGraph.observe()
        .states()
        .map(Graph::contents)
        .map(contents -> contents.stream().map(this::toTask).collect(toSet()));
  }

  @Override
  public Flowable<Set<Task>> tasksBlocking(Task blockedTask) {
    TaskImpl blockedTaskImpl = validateTask(blockedTask);
    return taskGraph.observe()
        .states()
        .map(graph -> graph.nodeOf(blockedTaskImpl.id()))
        .map(optionalNode ->
            optionalNode.map(DirectedGraph.DirectedNode::predecessors).orElse(Set.empty()))
        .map(predecessors ->
            predecessors.stream().map(Graph.Node::item).map(this::toTask).collect(toSet()));
  }

  @Override
  public Flowable<Set<Task>> tasksBlockedBy(Task blockingTask) {
    TaskImpl blockingTaskImpl = validateTask(blockingTask);
    return taskGraph.observe()
        .states()
        .map(graph -> graph.nodeOf(blockingTaskImpl.id()))
        .map(optionalNode ->
            optionalNode.map(DirectedGraph.DirectedNode::successors).orElse(Set.empty()))
        .map(predecessors ->
            predecessors.stream().map(Graph.Node::item).map(this::toTask).collect(toSet()));
  }

  @Override
  public Completable mutateTask(Task task, Function<? super TaskMutator, ? extends TaskMutator> mutation) {
    TaskImpl taskImpl = validateTask(task);
    return Single.just(new TaskMutatorImpl(this, taskImpl.id()))
        .<TaskMutator>map(mutation::apply)
        .flatMapCompletable(mutator -> Completable.fromAction(() -> applyMutator(mutator)));
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
    TaskImpl taskImpl = (TaskImpl) task;
    if (taskImpl.store() != this) {
      throw new IllegalArgumentException(
          "Task associated with another store. Expected <"
              + this
              + ">, received <"
              + taskImpl.store()
              + ">: "
              + taskImpl);
    }
    return taskImpl;
  }

  private Task toTask(TaskId id) {
    return new TaskImpl(this, id);
  }

  private void applyMutator(TaskMutator mutator) {
    TaskMutatorImpl mutatorImpl = validateMutator(mutator);
    applyMutatorToTaskData(mutatorImpl);
    applyMutatorToTaskGraph(mutatorImpl);
  }

  private void applyMutatorToTaskData(TaskMutatorImpl mutatorImpl) {
    Optional.of(mutatorImpl)
        .filter(mutator -> mutator.completed().isPresent() && mutator.label().isPresent())
        .map(TaskMutatorImpl::id)
        .flatMap(taskData::valueOf)
        .map(data ->
            new TaskData(
                mutatorImpl.completed().orElse(data.completed()),
                mutatorImpl.label().orElse(data.label())))
        .ifPresent(data -> taskData.putMapping(mutatorImpl.id(), data));
  }

  private void applyMutatorToTaskGraph(TaskMutatorImpl mutatorImpl) {
    TaskId id = mutatorImpl.id();
    mutatorImpl.blockingTasksToAdd()
        .forEach(blockingId -> taskGraph.addEdge(blockingId, id));
    mutatorImpl.blockingTasksToRemove()
        .forEach(blockingId -> taskGraph.removeEdge(blockingId, id));
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

  @Override
  public Completable writeToDisk() {
    return taskGraph.observe()
        .states()
        .firstOrError()
        .map(graph -> (DirectedGraph<TaskId>) graph)
        .zipWith(Single.just((Map<TaskId, TaskData>) ImmutableMap.copyOf(taskData)), Pair::of)
        .map(pair -> Completable.fromAction(() -> fileStore.storeBlocking(pair)))
        .flatMapCompletable(c -> c);
  }
}
