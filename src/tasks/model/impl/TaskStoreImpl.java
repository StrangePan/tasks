package tasks.model.impl;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableSet;

import java.util.Objects;
import java.util.Optional;
import omnia.algorithm.SetAlgorithms;
import omnia.data.cache.Memoized;
import omnia.data.cache.MemoizedInt;
import omnia.data.cache.WeakCache;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Graph;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.model.Task;
import tasks.model.TaskId;
import tasks.model.TaskStore;

final class TaskStoreImpl implements TaskStore {

  private final WeakCache<TaskIdImpl, TaskImpl> cache = new WeakCache<>();

  final ImmutableDirectedGraph<TaskIdImpl> graph;
  final ImmutableMap<TaskIdImpl, TaskData> data;

  private final Memoized<ImmutableSet<TaskImpl>> allTasks;
  private final Memoized<ImmutableSet<TaskImpl>> allUnblockedOpenTasks;
  private final Memoized<ImmutableSet<TaskImpl>> allBlockedOpenTasks;
  private final Memoized<ImmutableSet<TaskImpl>> allCompletedTasks;
  private final Memoized<ImmutableSet<TaskImpl>> allOpenTasks;
  private final Memoized<ImmutableDirectedGraph<TaskImpl>> taskGraph;

  private final MemoizedInt hash;

  TaskStoreImpl(ImmutableDirectedGraph<TaskIdImpl> graph, ImmutableMap<TaskIdImpl, TaskData> data) {
    this.graph = requireNonNull(graph);
    this.data = requireNonNull(data);

    if (graph.contents().count() != data.keys().count()) {
      throw new IllegalArgumentException("task graph and task data sizes don't match. ");
    }
    if (SetAlgorithms.differenceBetween(graph.contents(), data.keys()).isPopulated()) {
      throw new IllegalArgumentException("tasks in graph do not match tasks in data set");
    }

    allTasks = memoize(() -> data.keys().stream().map(this::toTask).collect(toImmutableSet()));

    allUnblockedOpenTasks =
        memoize(
            () -> graph.contents()
                .stream()
                .filter(this::isOpen)
                .filter(this::isUnblocked)
                .map(this::toTask)
                .collect(toImmutableSet()));

    allBlockedOpenTasks =
        memoize(
            () -> graph.contents()
                .stream()
                .filter(this::isOpen)
                .filter(this::isBlocked)
                .map(this::toTask)
                .collect(toImmutableSet()));

    allCompletedTasks =
        memoize(
            () -> data.entries()
                .stream()
                .filter(entry -> entry.value().isCompleted())
                .map(Map.Entry::key)
                .map(this::toTask)
                .collect(toImmutableSet()));

    allOpenTasks =
        memoize(
            () -> data.entries()
                .stream()
                .filter(entry -> !entry.value().isCompleted())
                .map(Map.Entry::key)
                .map(this::toTask)
                .collect(toImmutableSet()));

    taskGraph = memoize(() -> ImmutableDirectedGraph.copyOf(graph, this::toTask));

    hash = MemoizedInt.memoize(() -> Objects.hash(graph, data));
  }

  @Override
  public Optional<TaskImpl> lookUpById(long id) {
    return lookUpById(new TaskIdImpl(id));
  }

  @Override
  public Optional<TaskImpl> lookUpById(TaskId id) {
    return id instanceof TaskIdImpl ? lookUpById((TaskIdImpl) id) : Optional.empty();
  }

  private Optional<TaskImpl> lookUpById(TaskIdImpl id) {
    return graph.contents().contains(id) ? Optional.of(toTask(id)) : Optional.empty();
  }

  @Override
  public ImmutableSet<TaskImpl> allTasks() {
    return allTasks.value();
  }

  @Override
  public ImmutableSet<TaskImpl> allOpenTasksWithoutOpenBlockers() {
    return allUnblockedOpenTasks.value();
  }

  @Override
  public ImmutableSet<TaskImpl> allOpenTasksWithOpenBlockers() {
    return allBlockedOpenTasks.value();
  }

  @Override
  public ImmutableSet<TaskImpl> allCompletedTasks() {
    return allCompletedTasks.value();
  }

  @Override
  public ImmutableSet<TaskImpl> allOpenTasks() {
    return allOpenTasks.value();
  }

  @Override
  public ImmutableSet<TaskImpl> allTasksMatchingCliPrefix(String prefix) {
    requireNonNull(prefix);
    return graph.contents()
        .stream()
        .filter(id -> id.toString().regionMatches(0, prefix, 0, prefix.length()))
        .map(this::toTask)
        .collect(toImmutableSet());
  }

  @Override
  public ImmutableDirectedGraph<TaskImpl> taskGraph() {
    return taskGraph.value();
  }


  @Override
  public int hashCode() {
    return hash.value();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this
        || (obj instanceof TaskStoreImpl
            && Objects.equals(graph, ((TaskStoreImpl) obj).graph)
            && Objects.equals(data, ((TaskStoreImpl) obj).data));
  }

  ImmutableSet<TaskImpl> allTasksBlocking(TaskIdImpl id) {
    return graph.nodeOf(id)
        .map(DirectedGraph.DirectedNode::predecessors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node::item)
        .map(this::toTask)
        .collect(toImmutableSet());
  }

  ImmutableSet<TaskImpl> allTasksBlockedBy(TaskIdImpl id) {
    return graph.nodeOf(id)
        .map(DirectedGraph.DirectedNode::successors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node::item)
        .map(this::toTask)
        .collect(toImmutableSet());
  }

  ImmutableSet<TaskImpl> allOpenTasksBlocking(TaskIdImpl id) {
    return graph.nodeOf(id)
        .map(DirectedGraph.DirectedNode::predecessors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node::item)
        .map(this::toTask)
        .filter(task -> !task.isCompleted())
        .collect(toImmutableSet());
  }

  TaskImpl toTask(TaskIdImpl id) {
    return cache.getOrCache(
        id,
        () -> new TaskImpl(
            this,
            id,
            data.valueOf(id)
                .orElseThrow(() -> new IllegalArgumentException("unrecognized TaskIdImpl " + id))));
  }

  TaskImpl validateTask(Task task) {
    if (!(task instanceof TaskImpl)) {
      throw new IllegalArgumentException("unrecognized task type: " + task);
    }
    TaskIdImpl id = ((TaskImpl) task).id();
    if (!graph.contents().contains(id)) {
      throw new IllegalArgumentException("unrecognized TaskIdImpl: " + id);
    }
    return (TaskImpl) task;
  }

  private boolean isOpen(TaskIdImpl id) {
    return data.valueOf(id).map(taskData -> !taskData.isCompleted()).orElse(false);
  }

  private boolean isUnblocked(TaskIdImpl id) {
    return !isBlocked(id);
  }

  private boolean isBlocked(TaskIdImpl id) {
    return graph.nodeOf(id)
        .map(DirectedGraph.DirectedNode::predecessors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node::item)
        .map(data::valueOf)
        .anyMatch(
            taskDataOptional ->
                taskDataOptional.map(taskData -> !taskData.isCompleted()).orElse(false));
  }
}
