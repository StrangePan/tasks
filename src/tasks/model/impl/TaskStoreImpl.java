package tasks.model.impl;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static omnia.data.stream.Collectors.toImmutableSet;

import java.util.Objects;
import java.util.Optional;
import omnia.algorithm.SetAlgorithms;
import omnia.data.cache.Memoized;
import omnia.data.cache.MemoizedInt;
import omnia.data.structure.DirectedGraph;
import omnia.data.structure.Graph;
import omnia.data.structure.Map;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import tasks.model.TaskStore;

final class TaskStoreImpl implements TaskStore {

  private final ImmutableDirectedGraph<TaskId> graph;
  private final ImmutableMap<TaskId, TaskData> data;

  private final Memoized<ImmutableSet<TaskImpl>> allTasks;
  private final Memoized<ImmutableSet<TaskImpl>> allOpenTasksWithoutOpenBlockers;
  private final Memoized<ImmutableSet<TaskImpl>> allOpenTasksWithOpenBlockers;
  private final Memoized<ImmutableSet<TaskImpl>> allCompletedTasks;
  private final Memoized<ImmutableSet<TaskImpl>> allOpenTasks;
  private final MemoizedInt hash;

  TaskStoreImpl(ImmutableDirectedGraph<TaskId> graph, ImmutableMap<TaskId, TaskData> data) {
    this.graph = requireNonNull(graph);
    this.data = requireNonNull(data);

    if (graph.contents().count() != data.keys().count()) {
      throw new IllegalArgumentException("task graph and task data sizes don't match. ");
    }
    if (SetAlgorithms.differenceBetween(graph.contents(), data.keys()).isPopulated()) {
      throw new IllegalArgumentException("tasks in graph do not match tasks in data set");
    }

    allTasks = memoize(() -> data.keys().stream().map(this::toTask).collect(toImmutableSet()));

    allOpenTasksWithoutOpenBlockers =
        memoize(
            () -> graph.contents()
                .stream()
                .filter(this::hasNoOpenBlockers)
                .map(this::toTask)
                .collect(toImmutableSet()));

    allOpenTasksWithOpenBlockers =
        memoize(
            () -> graph.contents()
                .stream()
                .filter(this::hasAnyOpenBlockers)
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

    hash = MemoizedInt.memoize(() -> Objects.hash(graph, data));
  }

  @Override
  public Optional<TaskImpl> lookUpById(long id) {
    return Optional.empty();
  }

  @Override
  public ImmutableSet<TaskImpl> allTasks() {
    return allTasks.value();
  }

  @Override
  public ImmutableSet<TaskImpl> allOpenTasksWithoutOpenBlockers() {
    return allOpenTasksWithoutOpenBlockers.value();
  }

  @Override
  public ImmutableSet<TaskImpl> allOpenTasksWithOpenBlockers() {
    return allOpenTasksWithOpenBlockers.value();
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

  ImmutableSet<TaskImpl> allTasksBlocking(TaskId id) {
    return graph.nodeOf(id)
        .map(DirectedGraph.DirectedNode::predecessors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node::item)
        .map(this::toTask)
        .collect(toImmutableSet());
  }

  ImmutableSet<TaskImpl> allTasksBlockedBy(TaskId id) {
    return graph.nodeOf(id)
        .map(DirectedGraph.DirectedNode::successors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node::item)
        .map(this::toTask)
        .collect(toImmutableSet());
  }

  ImmutableSet<TaskImpl> allOpenTasksBlocking(TaskId id) {
    return graph.nodeOf(id)
        .map(DirectedGraph.DirectedNode::predecessors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node::item)
        .map(this::toTask)
        .filter(task -> !task.isCompleted())
        .collect(toImmutableSet());
  }

  TaskImpl toTask(TaskId id) {
    return new TaskImpl(
        this,
        id,
        data.valueOf(id)
            .orElseThrow(() -> new IllegalArgumentException("unrecognized TaskId " + id)));
  }

  private boolean hasNoOpenBlockers(TaskId id) {
    return !hasAnyOpenBlockers(id);
  }

  private boolean hasAnyOpenBlockers(TaskId id) {
    return graph.nodeOf(id)
        .map(DirectedGraph.DirectedNode::predecessors)
        .orElse(ImmutableSet.empty())
        .stream()
        .map(Graph.Node::item)
        .map(data::valueOf)
        .map(taskData -> taskData.map(TaskData::isCompleted).orElse(false))
        .anyMatch(isCompleted -> !isCompleted);
  }
}
