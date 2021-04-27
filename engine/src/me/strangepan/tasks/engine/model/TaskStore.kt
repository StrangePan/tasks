package me.strangepan.tasks.engine.model

import java.util.Optional
import omnia.data.structure.immutable.ImmutableDirectedGraph
import omnia.data.structure.immutable.ImmutableSet

interface TaskStore {
  fun lookUpById(id: Long): Optional<out Task>
  fun lookUpById(id: TaskId): Optional<out Task>
  fun allTasks(): ImmutableSet<out Task>
  fun allOpenTasksWithoutOpenBlockers(): ImmutableSet<out Task>
  fun allOpenTasksWithOpenBlockers(): ImmutableSet<out Task>
  fun allCompletedTasks(): ImmutableSet<out Task>
  fun allOpenTasks(): ImmutableSet<out Task>
  fun allTasksMatchingCliPrefix(prefix: String): ImmutableSet<out Task>
  fun taskGraph(): ImmutableDirectedGraph<out Task>
}