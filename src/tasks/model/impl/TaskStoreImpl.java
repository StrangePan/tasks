package tasks.model.impl;

import static java.util.Objects.requireNonNull;
import static omnia.data.stream.Collectors.toImmutableList;
import static omnia.data.stream.Collectors.toImmutableSet;
import static omnia.data.stream.Collectors.toSet;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.CompletableSubject;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import omnia.algorithm.GraphAlgorithms;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.DirectedGraph.DirectedNode;
import omnia.data.structure.Graph;
import omnia.data.structure.Map;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableDirectedGraph;
import omnia.data.structure.mutable.MutableMap;
import omnia.data.structure.observable.writable.WritableObservableDirectedGraph;
import omnia.data.structure.observable.writable.WritableObservableMap;
import omnia.data.structure.tuple.Couple;
import omnia.data.structure.tuple.Tuple;
import tasks.io.File;
import tasks.model.Task;
import tasks.model.TaskBuilder;
import tasks.model.TaskMutator;
import tasks.model.TaskStore;

/** This is currently NOT thread-safe. */
public final class TaskStoreImpl implements TaskStore {

  private final CompletableSubject isShutdown = CompletableSubject.create();
  private final Completable isShutdownComplete;

  private final TaskFileSource fileSource;
  private final WritableObservableDirectedGraph<TaskId> taskGraph;
  private final WritableObservableMap<TaskId, TaskData> taskData;

  private final Observable<Flowable<DirectedGraph<TaskId>>> currentTaskGraphSource;
  private final Observable<Flowable<Map<TaskId, TaskData>>> currentTaskDataSource;

  public TaskStoreImpl(String filePath) {
    this.fileSource = new TaskFileSource(File.fromPath(filePath));
    Couple<DirectedGraph<TaskId>, Map<TaskId, TaskData>> loadedData =
        fileSource.readFromFile().blockingGet();
    taskGraph = WritableObservableDirectedGraph.copyOf(loadedData.first());
    taskData = WritableObservableMap.copyOf(loadedData.second());

    currentTaskGraphSource = selectWhileRunning(taskGraph.observe().states());
    currentTaskDataSource = selectWhileRunning(taskData.observe().states());

    isShutdownComplete = isShutdown.hide().andThen(writeToDisk()).cache();
  }

  private <T> Observable<Flowable<T>> selectWhileRunning(Flowable<? extends T> starter) {
    return Observable.just(starter.map(i -> (T) i))
        .concatWith(isShutdown.hide().andThen(Observable.just(Flowable.empty())))
        .replay(1)
        .autoConnect(0);
  }

  private Flowable<DirectedGraph<TaskId>> observeTaskGraph() {
    return currentTaskGraphSource.toFlowable(BackpressureStrategy.LATEST).switchMap(f -> f);
  }

  private Flowable<Map<TaskId, TaskData>> observeTaskData() {
    return currentTaskDataSource.toFlowable(BackpressureStrategy.LATEST).switchMap(f -> f);
  }

  Flowable<Optional<TaskData>> lookUp(TaskId id) {
    return observeTaskData().map(state -> state.valueOf(id));
  }

  @Override
  public Maybe<Task> lookUpById(long id) {
    return observeTaskGraph()
        .firstElement()
        .map(graph -> graph.nodeOf(new TaskId(id)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(DirectedGraph.Node::item)
        .map(this::toTask);
  }

  @Override
  public Flowable<Set<Task>> allTasks() {
    return observeTaskGraph()
        .map(Graph::contents)
        .map(contents -> contents.stream().map(this::toTask).collect(toSet()));
  }

  @Override
  public Flowable<Set<Task>> allTasksBlocking(Task blockedTask) {
    TaskImpl blockedTaskImpl = validateTask(blockedTask);
    return observeTaskGraph()
        .map(graph -> graph.nodeOf(blockedTaskImpl.id()))
        .map(optionalNode -> optionalNode.map(DirectedNode::predecessors).orElse(Set.empty()))
        .map(
            predecessors ->
                predecessors.stream().map(Graph.Node::item).map(this::toTask).collect(toSet()));
  }

  @Override
  public Flowable<Set<Task>> allTasksBlockedBy(Task blockingTask) {
    TaskImpl blockingTaskImpl = validateTask(blockingTask);
    return observeTaskGraph()
        .map(graph -> graph.nodeOf(blockingTaskImpl.id()))
        .map(optionalNode -> optionalNode.map(DirectedNode::successors).orElse(Set.empty()))
        .map(
            successors ->
                successors.stream().map(Graph.Node::item).map(this::toTask).collect(toSet()));
  }

  @Override
  public Flowable<Set<Task>> allOpenTasksWithoutOpenBlockers() {
    return tasksFromNodesMatching(node -> !hasBlockingTasks(node) && !isCompleted(node));
  }

  @Override
  public Flowable<Set<Task>> allOpenTasksWithOpenBlockers() {
    return tasksFromNodesMatching(node -> hasBlockingTasks(node) && !isCompleted(node));
  }

  @Override
  public Flowable<Set<Task>> allCompletedTasks() {
    return tasksFromNodesMatching(this::isCompleted);
  }

  @Override
  public Flowable<Set<Task>> allOpenTasks() {
    return tasksFromNodesMatching(n -> !isCompleted(n));
  }

  @Override
  public Flowable<Set<Task>> allTasksMatchingCliPrefix(String prefix) {
    requireNonNull(prefix);
    return observeTaskGraph()
        .map(Graph::contents)
        .map(contents ->
            contents.stream()
                .filter(id -> id.toString().regionMatches(0, prefix, 0, prefix.length()))
                .map(this::toTask)
                .collect(toImmutableSet()));
  }

  @Override
  public Flowable<DirectedGraph<Task>> taskGraph() {
    return observeTaskGraph().map(this::toTaskGraph);
  }

  private boolean isCompleted(Graph.Node<? extends TaskId> node) {
    return taskData.valueOf(node.item()).get().isCompleted();
  }

  private boolean hasBlockingTasks(DirectedNode<? extends TaskId> node) {
    return node.predecessors()
        .stream()
        .anyMatch(
            successor ->
                taskData.valueOf(successor.item()).map(data -> !data.isCompleted()).orElse(false));
  }

  private Flowable<Set<Task>> tasksFromNodesMatching(
      Predicate<? super DirectedNode<? extends TaskId>> filter) {
    return observeTaskGraph()
        .map(DirectedGraph::nodes)
        .map(Set::stream)
        .map(stream ->
            stream.filter(filter)
                .map(Graph.Node::item)
                .map(this::toTask)
                .collect(toSet()));
  }

  @Override
  public Single<Task> createTask(
      String label, Function<? super TaskBuilder, ? extends TaskBuilder> builder) {
    return Single.just(new TaskBuilderImpl(this, label))
        .<TaskBuilder>map(builder::apply)
        .flatMap(taskBuilder -> Single.fromCallable(() -> maybeApplyBuilder(taskBuilder)))
        .map(this::toTask)
        .cache();
  }

  /**
   * Attempts to apply the given {@link TaskBuilder} to the task graph, but in a manner that tries
   * to preserve internal consistency by first applying the changes to a snapshot and validating
   * the snapshot.
   */
  private TaskId maybeApplyBuilder(TaskBuilder builder) {
    TaskBuilderImpl builderImpl = validateBuilder(builder);
    TaskData data = new TaskData(builderImpl.completed(), builderImpl.label());
    TaskId id = generateId();

    // check that the mutation is valid
    MutableMap<TaskId, TaskData> predictiveTaskData = HashMap.copyOf(taskData);
    MutableDirectedGraph<TaskId> predictiveTaskGraph =
        WritableObservableDirectedGraph.copyOf(taskGraph);
    applyBuilderTo(predictiveTaskData, predictiveTaskGraph, id, data, builderImpl);
    assertIsValid(predictiveTaskData, predictiveTaskGraph);

    applyBuilderTo(taskData, taskGraph, id, data, builderImpl);
    return id;
  }

  private static void applyBuilderTo(
      MutableMap<TaskId, TaskData> taskData,
      MutableDirectedGraph<TaskId> taskGraph,
      TaskId id,
      TaskData data,
      TaskBuilderImpl builder) {
    taskData.putMapping(id, data);
    taskGraph.addNode(id);
    builder.blockingTasks().forEach(blockingTask -> taskGraph.addEdge(blockingTask, id));
    builder.blockedTasks().forEach(blockedTask -> taskGraph.addEdge(id, blockedTask));
  }

  private static void assertIsValid(
      Map<TaskId, TaskData> taskData,
      DirectedGraph<TaskId> taskGraph) {
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
              throw new CyclicalDependencyException(
                  "Cycle detected",
                  cycle);
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

  private TaskId generateId() {
    // TODO(vxi4873454w0): more sophisticated algorithm
    while (true) {
      TaskId id = new TaskId((long) (Math.random() * TaskId.MAX_ID_VALUE));
      if (!taskGraph.contents().contains(id)) {
        return id;
      }
    }
  }

  @Override
  public Completable mutateTask(
      Task task, Function<? super TaskMutator, ? extends TaskMutator> mutation) {
    return mutateTask(validateTask(task), mutation);
  }

  Completable mutateTask(
      TaskImpl task, Function<? super TaskMutator, ? extends TaskMutator> mutation) {
    return Single.just(new TaskMutatorImpl(this, task.id()))
        .<TaskMutator>map(mutation::apply)
        .flatMapCompletable(mutator -> Completable.fromAction(() -> maybeApplyMutator(mutator)))
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

  private ImmutableDirectedGraph<Task> toTaskGraph(DirectedGraph<TaskId> idGraph) {
    ImmutableDirectedGraph.Builder<Task> taskGraph = ImmutableDirectedGraph.builder();
    idGraph.nodes().forEach(node -> taskGraph.addNode(toTask(node.item())));
    idGraph.edges().forEach(
        edge -> taskGraph.addEdge(toTask(edge.start().item()), toTask(edge.end().item())));
    return taskGraph.build();
  }

  private void maybeApplyMutator(TaskMutator mutator) {
    TaskMutatorImpl mutatorImpl = validateMutator(mutator);

    MutableMap<TaskId, TaskData> predictiveTaskData = HashMap.copyOf(taskData);
    MutableDirectedGraph<TaskId> predictiveTaskGraph =
        WritableObservableDirectedGraph.copyOf(taskGraph);
    applyMutatorTo(mutatorImpl, predictiveTaskData);
    applyMutatorTo(mutatorImpl, predictiveTaskGraph);
    assertIsValid(predictiveTaskData, predictiveTaskGraph);

    applyMutatorTo(mutatorImpl, taskData);
    applyMutatorTo(mutatorImpl, taskGraph);
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

  private void applyMutatorTo(TaskMutatorImpl mutatorImpl, MutableMap<TaskId, TaskData> taskData) {
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

  private void applyMutatorTo(TaskMutatorImpl mutatorImpl, MutableDirectedGraph<TaskId> taskGraph) {
    TaskId id = mutatorImpl.id();

    if (mutatorImpl.overwriteBlockingTasks()) {
      taskGraph.nodeOf(id)
          .map(DirectedNode::incomingEdges)
          .map(ImmutableSet::copyOf)
          .orElse(ImmutableSet.empty())
          .forEach(edge -> taskGraph.removeEdge(edge.start(), edge.end()));
    }
    mutatorImpl.blockingTasksToAdd()
        .forEach(blockingId -> taskGraph.addEdge(blockingId, id));
    mutatorImpl.blockingTasksToRemove()
        .forEach(blockingId -> taskGraph.removeEdge(blockingId, id));

    if (mutatorImpl.overwriteBlockedTasks()) {
      taskGraph.nodeOf(id)
          .map(DirectedNode::outgoingEdges)
          .map(ImmutableSet::copyOf)
          .orElse(ImmutableSet.empty())
          .forEach(edge -> taskGraph.removeEdge(edge.start(), edge.end()));
    }
    mutatorImpl.blockedTasksToAdd()
        .forEach(blockedId -> taskGraph.addEdge(id, blockedId));
    mutatorImpl.blockedTasksToRemove()
        .forEach(blockedId -> taskGraph.removeEdge(id, blockedId));
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
    return Single.zip(
            observeTaskGraph().firstOrError(),
            observeTaskData().firstOrError(),
            Tuple::of)
        .flatMapCompletable(fileSource::writeToFile)
        .cache();
  }

  @Override
  public Completable shutdown() {
    return isShutdownComplete;
  }
}
