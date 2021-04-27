package me.strangepan.tasks.engine.model.impl

import io.reactivex.rxjava3.core.Observable
import java.util.Comparator
import java.util.Objects
import java.util.Optional
import java.util.function.Supplier
import omnia.algorithm.SetAlgorithms.differenceBetween
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.cache.MemoizedInt
import omnia.data.cache.WeakCache
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.immutable.ImmutableDirectedGraph
import omnia.data.structure.immutable.ImmutableDirectedGraph.Companion.copyOf
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.immutable.ImmutableSortedSet
import omnia.data.structure.immutable.ImmutableSortedSet.Companion.copyOf
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskId
import me.strangepan.tasks.engine.model.TaskStore
import me.strangepan.tasks.engine.util.rx.Observables

class TaskStoreImpl(val graph: ImmutableDirectedGraph<TaskIdImpl>, val data: ImmutableMap<TaskIdImpl, TaskData>) : TaskStore {
  private val cache = WeakCache<TaskIdImpl, TaskImpl>()
  private val allTaskIds: Memoized<ImmutableSortedSet<TaskIdImpl>>
  private val allTasks: Memoized<ImmutableSet<TaskImpl>>
  private val allUnblockedOpenTasks: Memoized<ImmutableSet<TaskImpl>>
  private val allBlockedOpenTasks: Memoized<ImmutableSet<TaskImpl>>
  private val allCompletedTasks: Memoized<ImmutableSet<TaskImpl>>
  private val allOpenTasks: Memoized<ImmutableSet<TaskImpl>>
  private val taskGraph: Memoized<ImmutableDirectedGraph<TaskImpl>>
  private val hash: MemoizedInt

  override fun lookUpById(id: Long): Optional<TaskImpl> {
    return lookUpById(TaskIdImpl(id))
  }

  override fun lookUpById(id: TaskId): Optional<TaskImpl> {
    return if (id is TaskIdImpl) lookUpById(id) else Optional.empty()
  }

  private fun lookUpById(id: TaskIdImpl): Optional<TaskImpl> {
    return if (graph.contents().contains(id)) Optional.of(toTask(id)) else Optional.empty()
  }

  override fun allTasks(): ImmutableSet<TaskImpl> {
    return allTasks.value()
  }

  override fun allOpenTasksWithoutOpenBlockers(): ImmutableSet<TaskImpl> {
    return allUnblockedOpenTasks.value()
  }

  override fun allOpenTasksWithOpenBlockers(): ImmutableSet<TaskImpl> {
    return allBlockedOpenTasks.value()
  }

  override fun allCompletedTasks(): ImmutableSet<TaskImpl> {
    return allCompletedTasks.value()
  }

  override fun allOpenTasks(): ImmutableSet<TaskImpl> {
    return allOpenTasks.value()
  }

  override fun allTasksMatchingCliPrefix(prefix: String): ImmutableSet<TaskImpl> {
    Objects.requireNonNull(prefix)
    return graph.contents()
        .stream()
        .filter { id -> id.toString().regionMatches(0, prefix, 0, prefix.length) }
        .map(this::toTask)
        .collect(toImmutableSet())
  }

  override fun taskGraph(): ImmutableDirectedGraph<TaskImpl> {
    return taskGraph.value()
  }

  override fun hashCode(): Int {
    return hash.value()
  }

  override fun equals(other: Any?): Boolean {
    return (other === this
        || (other is TaskStoreImpl
        && graph == other.graph
        && data == other.data))
  }

  fun allTaskIds(): ImmutableSortedSet<TaskIdImpl> {
    return allTaskIds.value()
  }

  fun allTasksBlocking(id: TaskIdImpl): ImmutableSet<TaskImpl> {
    return graph.nodeOf(id)
        .map { node -> node.predecessors() }
        .orElse(ImmutableSet.empty())
        .stream()
        .map { node -> node.item() }
        .map(this::toTask)
        .collect(toImmutableSet())
  }

  fun allTasksBlockedBy(id: TaskIdImpl): ImmutableSet<TaskImpl> {
    return graph.nodeOf(id)
        .map { node -> node.successors() }
        .orElse(ImmutableSet.empty())
        .stream()
        .map { node -> node.item() }
        .map(this::toTask)
        .collect(toImmutableSet())
  }

  fun allOpenTasksBlocking(id: TaskIdImpl): ImmutableSet<TaskImpl> {
    return graph.nodeOf(id)
        .map { node -> node.predecessors() }
        .orElse(ImmutableSet.empty())
        .stream()
        .map(ImmutableDirectedGraph<TaskIdImpl>.DirectedNode::item)
        .map(this::toTask)
        .filter { task -> !task.status().isCompleted }
        .collect(toImmutableSet())
  }

  fun toTask(id: TaskIdImpl): TaskImpl {
    return cache.getOrCache(
        id
    ) {
      TaskImpl(
          this,
          id,
          data.valueOf(id)
              .orElseThrow { IllegalArgumentException("unrecognized TaskIdImpl $id") })
    }
  }

  fun validateTask(task: Task): TaskImpl {
    require(task is TaskImpl) { "unrecognized task type: $task" }
    val id = task.id()
    require(graph.contents().contains(id)) { "unrecognized TaskIdImpl: $id" }
    return task
  }

  private fun isOpen(id: TaskIdImpl): Boolean {
    return data.valueOf(id).map { taskData -> !taskData.status().isCompleted }.orElse(false)
  }

  private fun isUnblocked(id: TaskIdImpl): Boolean {
    return !isBlocked(id)
  }

  private fun isBlocked(id: TaskIdImpl): Boolean {
    return graph.nodeOf(id)
        .map { node -> node.predecessors() }
        .orElse(ImmutableSet.empty())
        .stream()
        .map { node -> node.item() }
        .map { key -> data.valueOf(key) }
        .anyMatch { taskDataOptional -> taskDataOptional.map { taskData: TaskData -> !taskData.status().isCompleted }.orElse(false) }
  }

  init {
    require(graph.contents().count() == data.keys().count()) { "task graph and task data sizes don't match. " }
    require(!differenceBetween(graph.contents(), data.keys()).isPopulated) { "tasks in graph do not match tasks in data set" }
    allTaskIds = memoize {
      Observable.fromIterable(data.keys())
          .to(Observables.toImmutableSet())
          .map { set -> copyOf(Comparator.comparingLong { obj: TaskIdImpl -> obj.asLong() }, set) }
          .blockingGet()
    }
    allTasks = memoize { data.keys().stream().map { id: TaskIdImpl -> toTask(id) }.collect(toImmutableSet()) }
    allUnblockedOpenTasks = memoize {
      graph.contents()
          .stream()
          .filter { id: TaskIdImpl -> isOpen(id) }
          .filter { id: TaskIdImpl -> isUnblocked(id) }
          .map { id: TaskIdImpl -> toTask(id) }
          .collect(toImmutableSet())
    }
    allBlockedOpenTasks = memoize {
      graph.contents()
          .stream()
          .filter { id: TaskIdImpl -> isOpen(id) }
          .filter { id: TaskIdImpl -> isBlocked(id) }
          .map { id: TaskIdImpl -> toTask(id) }
          .collect(toImmutableSet())
    }
    allCompletedTasks = memoize {
      data.entries()
          .stream()
          .filter { entry -> entry.value().status().isCompleted }
          .map { entry -> entry.key() }
          .map { id -> toTask(id) }
          .collect(toImmutableSet())
    }
    allOpenTasks = memoize {
      data.entries()
          .stream()
          .filter { entry -> !entry.value().status().isCompleted }
          .map { entry -> entry.key() }
          .map { id -> toTask(id) }
          .collect(toImmutableSet())
    }
    taskGraph = memoize(Supplier { copyOf(graph) { id: TaskIdImpl -> toTask(id) } })
    hash = MemoizedInt.memoize { Objects.hash(graph, data) }
  }
}