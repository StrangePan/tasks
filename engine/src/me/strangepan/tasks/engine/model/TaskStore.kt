package me.strangepan.tasks.engine.model

import omnia.data.structure.immutable.ImmutableDirectedGraph
import omnia.data.structure.immutable.ImmutableSet

/** An immutable read-only class for querying tasks. */
interface TaskStore {

  /** Search for a task via numerical representation of [TaskId].  */
  fun lookUpById(id: Long): Task?

  /** Search for a task via [TaskId]. */
  fun lookUpById(id: TaskId): Task?

  /** Retrieve all tasks in the store in no particular order. */
  fun allTasks(): ImmutableSet<out Task>

  /** Retrieves all tasks that are open and unblocked. */
  fun allOpenTasksWithoutOpenBlockers(): ImmutableSet<out Task>

  /** Retrieves all tasks that are open and blocked by at least one open task. */
  fun allOpenTasksWithOpenBlockers(): ImmutableSet<out Task>

  /** Retrieves all tasks that have been completed. */
  fun allCompletedTasks(): ImmutableSet<out Task>

  /** Retrieves all tasks that are open and not started. */
  fun allOpenTasks(): ImmutableSet<out Task>

  /** Retrieves all tasks whose string representation of [TaskId] starts with [prefix]. */
  fun allTasksMatchingCliPrefix(prefix: String): ImmutableSet<out Task>

  /**
   * Retrieves the graph representation of the dependency relationship between tasks.
   *
   * The returned [ImmutableDirectedGraph] is organized such that each blocking tasks are
   * predecessors and that blocked tasks are successors.
   */
  fun taskGraph(): ImmutableDirectedGraph<out Task>
}