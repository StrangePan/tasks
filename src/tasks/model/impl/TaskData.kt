package tasks.model.impl

import java.util.Objects
import tasks.model.Task

class TaskData(private val label: String, private val status: Task.Status) {
  fun label(): String {
    return label
  }

  fun status(): Task.Status {
    return status
  }

  override fun equals(other: Any?): Boolean {
    return other is TaskData && label == other.label && status == other.status
  }

  override fun hashCode(): Int {
    return Objects.hash(label, status)
  }

}