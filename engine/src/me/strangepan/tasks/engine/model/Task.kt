package me.strangepan.tasks.engine.model

import omnia.data.structure.Set

/** A read-only immutable model representing the state of a task and its core contents. */
interface Task {
  val store: TaskStore
  val id: TaskId
  val label: String
  val status: Status
  val isUnblocked: Boolean
  val blockingTasks: Set<out Task>
  val blockedTasks: Set<out Task>

  enum class Status {
    OPEN, COMPLETED, STARTED;

    val isOpen: Boolean
      get() = this == OPEN
    val isCompleted: Boolean
      get() = this == COMPLETED
    val isStarted: Boolean
      get() = this == STARTED
  }
}