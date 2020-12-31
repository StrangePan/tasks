package tasks.model.impl

import java.util.Objects
import java.util.function.Supplier
import kotlin.math.max
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.cli.out.Output.Companion.empty
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.SortedSet
import omnia.data.structure.immutable.ImmutableSet
import tasks.model.Task

class TaskImpl(private val store: TaskStoreImpl, private val id: TaskIdImpl, private val data: TaskData) : Task {
  private val blockingTasks: Memoized<ImmutableSet<TaskImpl>> = memoize(Supplier { store.allTasksBlocking(id) })
  private val blockedTasks: Memoized<ImmutableSet<TaskImpl>> = memoize(Supplier { store.allTasksBlockedBy(id) })
  private val openBlockingTasks: Memoized<ImmutableSet<TaskImpl>> = memoize(Supplier { store.allOpenTasksBlocking(id) })

  override fun id(): TaskIdImpl {
    return id
  }

  override fun label(): String {
    return data.label()
  }

  override fun status(): Task.Status {
    return data.status()
  }

  override val isUnblocked: Boolean
    get() = !openBlockingTasks.value().isPopulated

  override fun blockingTasks(): ImmutableSet<TaskImpl> {
    return blockingTasks.value()
  }

  override fun blockedTasks(): ImmutableSet<TaskImpl> {
    return blockedTasks.value()
  }

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
    return render().toString()
  }

  override fun render(): Output {
    val allIds: SortedSet<TaskIdImpl> = store.allTaskIds()
    val precedingId = allIds.itemPreceding(id)
    val followingId = allIds.itemFollowing(id)
    val stringId = id.toString()
    val longestCommonPrefix = max(
        precedingId.map { other -> longestCommonPrefix(other.toString(), stringId) }.orElse(0),
        followingId.map { other -> longestCommonPrefix(other.toString(), stringId) }.orElse(0)) + 1
    return builder()
        .underlined()
        .color(Output.Color16.LIGHT_GREEN)
        .append(stringId.substring(0, longestCommonPrefix))
        .defaultUnderline()
        .append(stringId.substring(longestCommonPrefix))
        .defaultColor()
        .append(render(status()))
        .append(": ")
        .defaultColor()
        .append(label())
        .build()
  }

  companion object {
    private fun render(status: Task.Status): Output {
      return when (status) {
        Task.Status.STARTED -> builder()
            .color(Output.Color16.YELLOW)
            .append(" (started)")
            .build()
        Task.Status.COMPLETED -> builder()
            .color(Output.Color16.LIGHT_CYAN)
            .append(" (completed)")
            .build()
        else -> empty()
      }
    }

    private fun longestCommonPrefix(a: String, b: String): Int {
      var i = 0
      while (true) {
        if (i > a.length || i > b.length || a[i] != b[i]) {
          return i
        }
        i++
      }
    }
  }

}