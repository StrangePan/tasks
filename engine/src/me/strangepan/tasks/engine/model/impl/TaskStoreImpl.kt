package me.strangepan.tasks.engine.model.impl

import java.util.Comparator
import java.util.Objects
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
import me.strangepan.tasks.engine.model.TaskId
import me.strangepan.tasks.engine.model.TaskStore

class TaskStoreImpl(
    val graph: ImmutableDirectedGraph<TaskIdImpl>, val data: ImmutableMap<TaskIdImpl, TaskData>) :
    TaskStore {

  private val cache = WeakCache<TaskIdImpl, TaskImpl>()
  private val allTaskIds: Memoized<ImmutableSortedSet<TaskIdImpl>>
  private val allTasks: Memoized<ImmutableSet<TaskImpl>>
  private val allUnblockedOpenTasks: Memoized<ImmutableSet<TaskImpl>>
  private val allBlockedOpenTasks: Memoized<ImmutableSet<TaskImpl>>
  private val allCompletedTasks: Memoized<ImmutableSet<TaskImpl>>
  private val allOpenTasks: Memoized<ImmutableSet<TaskImpl>>
  private val taskGraph: Memoized<ImmutableDirectedGraph<TaskImpl>>
  private val hash: MemoizedInt

  override fun lookUpById(id: Long): TaskImpl? {
    return lookUpById(TaskIdImpl(id))
  }

  override fun lookUpById(id: TaskId): TaskImpl? {
    return if (id is TaskIdImpl) lookUpById(id) else null
  }

  private fun lookUpById(id: TaskIdImpl): TaskImpl? {
    return if (graph.contents().contains(id)) toTask(id) else null
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

  override fun allTaskIds(): ImmutableSortedSet<TaskIdImpl> {
    return allTaskIds.value()
  }

  fun allTasksBlocking(id: TaskIdImpl): ImmutableSet<TaskImpl> {
    return graph.nodeOf(id)
        .map { it.predecessors() }
        .orElse(ImmutableSet.empty())
        .stream()
        .map { it.item() }
        .map(this::toTask)
        .collect(toImmutableSet())
  }

  fun allTasksBlockedBy(id: TaskIdImpl): ImmutableSet<TaskImpl> {
    return graph.nodeOf(id)
        .map { it.successors() }
        .orElse(ImmutableSet.empty())
        .stream()
        .map { it.item() }
        .map(this::toTask)
        .collect(toImmutableSet())
  }

  fun allOpenTasksBlocking(id: TaskIdImpl): ImmutableSet<TaskImpl> {
    return graph.nodeOf(id)
        .map { it.predecessors() }
        .orElse(ImmutableSet.empty())
        .stream()
        .map { it.item() }
        .map(this::toTask)
        .filter { !it.status.isCompleted }
        .collect(toImmutableSet())
  }

  fun toTask(id: TaskIdImpl): TaskImpl {
    return cache.getOrCache(id) {
      TaskImpl(
          this,
          id,
          data.valueOf(id).orElseThrow { IllegalArgumentException("unrecognized TaskIdImpl $id") })
    }
  }

  private fun isOpen(id: TaskIdImpl): Boolean {
    return data.valueOf(id).map { !it.status().isCompleted }.orElse(false)
  }

  private fun isUnblocked(id: TaskIdImpl): Boolean {
    return !isBlocked(id)
  }

  private fun isBlocked(id: TaskIdImpl): Boolean {
    return graph.nodeOf(id)
        .map { it.predecessors() }
        .orElse(ImmutableSet.empty())
        .stream()
        .map { it.item() }
        .map(data::valueOf)
        .anyMatch {
            taskDataOptional -> taskDataOptional.map { !it.status().isCompleted }.orElse(false)
        }
  }

  init {
    require(graph.contents().count() == data.keys().count()) {
      "task graph and task data sizes don't match. "
    }
    require(!differenceBetween(graph.contents(), data.keys()).isPopulated) {
      "tasks in graph do not match tasks in data set"
    }
    allTaskIds = memoize {
      ImmutableSortedSet.copyOf(Comparator.comparingLong(TaskIdImpl::asLong), data.keys())
    }
    allTasks = memoize { data.keys().stream().map(this::toTask).collect(toImmutableSet()) }
    allUnblockedOpenTasks = memoize {
      graph.contents()
          .stream()
          .filter(this::isOpen)
          .filter(this::isUnblocked)
          .map(this::toTask)
          .collect(toImmutableSet())
    }
    allBlockedOpenTasks = memoize {
      graph.contents()
          .stream()
          .filter(this::isOpen)
          .filter(this::isBlocked)
          .map(this::toTask)
          .collect(toImmutableSet())
    }
    allCompletedTasks = memoize {
      data.entries()
          .stream()
          .filter { it.value().status().isCompleted }
          .map { it.key() }
          .map(this::toTask)
          .collect(toImmutableSet())
    }
    allOpenTasks = memoize {
      data.entries()
          .stream()
          .filter { !it.value().status().isCompleted }
          .map { it.key() }
          .map(this::toTask)
          .collect(toImmutableSet())
    }
    taskGraph = memoize { copyOf(graph, this::toTask) }
    hash = MemoizedInt.memoize { Objects.hash(graph, data) }
  }
}