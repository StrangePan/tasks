package me.strangepan.tasks.engine.model

import omnia.cli.out.Output
import omnia.data.structure.Set

interface Task {
  fun id(): TaskId
  fun label(): String
  fun status(): Status
  val isUnblocked: Boolean
  fun blockingTasks(): Set<out Task>
  fun blockedTasks(): Set<out Task>
  fun render(): Output
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