package me.strangepan.tasks.engine.model.impl

import java.util.Objects
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.immutable.ImmutableSet
import me.strangepan.tasks.engine.model.Task

class TaskImpl(
    override val store: TaskStoreImpl,
    override val id: TaskIdImpl,
    private val data: TaskData) : Task {

  private val memoizedBlockingTasks: Memoized<ImmutableSet<TaskImpl>> =
    memoize { store.allTasksBlocking(id) }
  private val memoizedBlockedTasks: Memoized<ImmutableSet<TaskImpl>> =
    memoize { store.allTasksBlockedBy(id) }
  private val openBlockingTasks: Memoized<ImmutableSet<TaskImpl>> =
    memoize { store.allOpenTasksBlocking(id) }

  override val label: String
    get() = data.label()

  override val status: Task.Status
    get() = data.status()

  override val isUnblocked: Boolean
    get() = !openBlockingTasks.value().isPopulated

  override val blockingTasks: ImmutableSet<TaskImpl>
    get() = memoizedBlockingTasks.value()

  override val blockedTasks: ImmutableSet<TaskImpl>
    get() = memoizedBlockedTasks.value()

  override fun equals(other: Any?): Boolean {
    return (other === this
        || (other is TaskImpl
        && id == other.id
        && store == other.store
        && data == other.data))
  }

  override fun hashCode(): Int {
    return Objects.hash(store, id, data)
  }

  override fun toString(): String {
    return "${id}${stringify(status)}: $label"
  }

  private fun stringify(status: Task.Status): String {
    return when (status) {
      Task.Status.STARTED -> " (started)"
      Task.Status.COMPLETED -> " (completed)"
      else -> ""
    }
  }
}