package tasks.model.impl;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableSet;
import static omnia.data.stream.Collectors.toSet;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Graph;
import omnia.data.structure.Map;
import omnia.data.structure.Pair;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.observable.writable.WritableObservableDirectedGraph;
import tasks.io.File;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.TaskMutator;
import tasks.model.TaskStore;

public final class TaskStoreImpl implements TaskStore {

  private final TaskFileSource fileSource; // TODO
  private final WritableObservableDirectedGraph<TaskId> taskGraph;
  private final MutableMap<TaskId, TaskData> taskData;

  public TaskStoreImpl(String filePath) {
    this.fileSource = new TaskFileSource(File.fromPath(filePath));
    Pair<DirectedGraph<TaskId>, Map<TaskId, TaskData>> loadedData =
        fileSource.readFromFile().blockingGet();
    taskGraph = WritableObservableDirectedGraph.copyOf(loadedData.first());
    taskData = HashMap.copyOf(loadedData.second());
  }

  Maybe<Flowable<TaskData>> lookUp(TaskId id) {
    return Single.just(taskData.valueOf(id))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(Flowable::just);
  }

  @Override
  public Maybe<Task> lookUpById(long id) {
    return taskGraph.observe()
        .states()
        .firstOrError()
        .map(graph -> graph.nodeOf(new TaskId(id)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(DirectedGraph.Node::item)
        .map(this::toTask);
  }

  @Override
  public Flowable<Set<Task>> allTasks() {
    return taskGraph.observe()
        .states()
        .map(Graph::contents)
        .map(contents -> contents.stream().map(this::toTask).collect(toSet()));
  }

  @Override
  public Flowable<Set<Task>> allTasksBlocking(Task blockedTask) {
    TaskImpl blockedTaskImpl = validateTask(blockedTask);
    return taskGraph.observe()
        .states()
        .map(graph -> graph.nodeOf(blockedTaskImpl.id()))
        .map(optionalNode ->
            optionalNode.map(DirectedGraph.DirectedNode::successors).orElse(Set.empty()))
        .map(successors ->
            successors.stream().map(Graph.Node::item).map(this::toTask).collect(toSet()));
  }

  @Override
  public Flowable<Set<Task>> allTasksBlockedBy(Task blockingTask) {
    TaskImpl blockingTaskImpl = validateTask(blockingTask);
    return taskGraph.observe()
        .states()
        .map(graph -> graph.nodeOf(blockingTaskImpl.id()))
        .map(optionalNode ->
            optionalNode.map(DirectedGraph.DirectedNode::predecessors).orElse(Set.empty()))
        .map(predecessors ->
            predecessors.stream().map(Graph.Node::item).map(this::toTask).collect(toSet()));
  }

  @Override
  public Flowable<Set<Task>> allTasksWithoutOpenBlockers() {
    return tasksFromNodesMatching(node -> !hasBlockingTasks(node) && !isCompleted(node));
  }

  @Override
  public Flowable<Set<Task>> allTasksWithOpenBlockers() {
    return tasksFromNodesMatching(node -> hasBlockingTasks(node) && !isCompleted(node));
  }

  @Override
  public Flowable<Set<Task>> completedTasks() {
    return tasksFromNodesMatching(this::isCompleted);
  }

  @Override
  public Flowable<Set<Task>> allTasksMatchingCliPrefix(String prefix) {
    requireNonNull(prefix);
    return taskGraph.observe()
        .states()
        .map(Graph::contents)
        .map(contents ->
            contents.stream()
                .filter(id -> id.toString().regionMatches(0, prefix, 0, prefix.length()))
                .map(this::toTask)
                .collect(toImmutableSet()));
  }

  private boolean isCompleted(Graph.Node<? extends TaskId> node) {
    return taskData.valueOf(node.item()).get().isCompleted();
  }

  private boolean hasBlockingTasks(DirectedGraph.DirectedNode<? extends TaskId> node) {
    return node.successors()
        .stream()
        .anyMatch(
            successor ->
                taskData.valueOf(successor.item()).map(data -> !data.isCompleted()).orElse(false));
  }

  private Flowable<Set<Task>> tasksFromNodesMatching(Predicate<? super DirectedGraph.DirectedNode<? extends TaskId>> filter) {
    return taskGraph.observe()
        .states()
        .map(DirectedGraph::nodes)
        .map(Set::stream)
        .map(stream ->
            stream.filter(filter)
                .map(Graph.Node::item)
                .map(this::toTask)
                .collect(toSet()));
  }

  @Override
  public Completable createTask(String label, Function<? super TaskBuilder, ? extends TaskBuilder> builder) {
    return Single.just(new TaskBuilderImpl(this, label))
        .<TaskBuilder>map(builder::apply)
        .flatMapCompletable(taskBuilder -> Completable.fromAction(() -> applyBuilder(taskBuilder)))
        .cache();
  }

  private void applyBuilder(TaskBuilder builder) {
    TaskBuilderImpl builderImpl = validateBuilder(builder);
    TaskData data = new TaskData(builderImpl.completed(), builderImpl.label());
    TaskId id = generateId();
    taskData.putMapping(id, data);
    taskGraph.addNode(id);
    builderImpl.blockingTasks().forEach(blockingTask -> taskGraph.addEdge(id, blockingTask));
    builderImpl.blockedTasks().forEach(blockedTask -> taskGraph.addEdge(blockedTask, id));
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

  private TaskId generateId() {
    // TODO: more sophisticated algorithm
    TaskId id;
    do {
      id = new TaskId((long) (Math.random() * Long.MAX_VALUE));
    } while (taskGraph.contents().contains(id));
    return id;
  }

  @Override
  public Completable mutateTask(Task task, Function<? super TaskMutator, ? extends TaskMutator> mutation) {
    return mutateTask(validateTask(task), mutation);
  }

  Completable mutateTask(TaskImpl task, Function<? super TaskMutator, ? extends TaskMutator> mutation) {
    return Single.just(new TaskMutatorImpl(this, task.id()))
        .<TaskMutator>map(mutation::apply)
        .flatMapCompletable(mutator -> Completable.fromAction(() -> applyMutator(mutator)))
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

  private void applyMutatorToTaskData(TaskMutatorImpl mutatorImpl) {
    Optional.of(mutatorImpl)
        .filter(mutator -> mutator.completed().isPresent() || mutator.label().isPresent())
        .map(TaskMutatorImpl::id)
        .flatMap(taskData::valueOf)
        .map(data ->
            new TaskData(
                mutatorImpl.completed().orElse(data.isCompleted()),
                mutatorImpl.label().orElse(data.label())))
        .ifPresent(data -> taskData.putMapping(mutatorImpl.id(), data));
  }

  private void applyMutatorToTaskGraph(TaskMutatorImpl mutatorImpl) {
    TaskId id = mutatorImpl.id();

    if (mutatorImpl.overwriteBlockingTasks()) {
      taskGraph.nodeOf(id)
          .map(DirectedGraph.DirectedNode::outgoingEdges)
          .map(ImmutableSet::copyOf)
          .orElse(ImmutableSet.empty())
          .forEach(edge -> taskGraph.removeEdge(edge.start(), edge.end()));
    }
    mutatorImpl.blockingTasksToAdd()
        .forEach(blockingId -> taskGraph.addEdge(id, blockingId));
    mutatorImpl.blockingTasksToRemove()
        .forEach(blockingId -> taskGraph.removeEdge(id, blockingId));

    if (mutatorImpl.overwriteBlockedTasks()) {
      taskGraph.nodeOf(id)
          .map(DirectedGraph.DirectedNode::incomingEdges)
          .map(ImmutableSet::copyOf)
          .orElse(ImmutableSet.empty())
          .forEach(edge -> taskGraph.removeEdge(edge.start(), edge.end()));
    }
    mutatorImpl.blockedTasksToAdd()
        .forEach(blockedId -> taskGraph.addEdge(blockedId, id));
    mutatorImpl.blockedTasksToRemove()
        .forEach(blockedId -> taskGraph.removeEdge(blockedId, id));
  }

  @Override
  public Completable deleteTask(Task task) {
    return deleteTask(validateTask(task));
  }

  public Completable deleteTask(TaskImpl task) {
    return Completable.mergeArray(
        Completable.fromAction(() -> taskGraph.removeNode(task.id())),
        Completable.fromAction(() -> taskData.removeKey(task.id())))
        .cache();
  }

  @Override
  public Completable writeToDisk() {
    return taskGraph.observe()
        .states()
        .firstOrError()
        .map(graph -> (DirectedGraph<TaskId>) graph)
        .zipWith(Single.just((Map<TaskId, TaskData>) ImmutableMap.copyOf(taskData)), Pair::of)
        .flatMapCompletable(fileSource::writeToFile)
        .cache();
  }
}
