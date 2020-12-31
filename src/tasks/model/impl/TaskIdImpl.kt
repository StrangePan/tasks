package tasks.model.impl

import java.util.Objects
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import omnia.contract.TypedContainer
import tasks.model.TaskId

class TaskIdImpl(private val id: Long) : TaskId {

  override fun equals(other: Any?): Boolean {
    return other is TaskIdImpl && other.id == id
  }

  override fun hashCode(): Int {
    return Objects.hash(id)
  }

  override fun toString(): String {
    return forceToMaxLength(asLong().toString(TO_STRING_BASE))
  }

  fun asLong(): Long {
    return id
  }

  companion object {
    const val TO_STRING_BASE = Character.MAX_RADIX
    val TO_STRING_MAX_LENGTH = floor(log(Long.MAX_VALUE.toDouble(), TO_STRING_BASE.toDouble())).toInt()
    val MAX_ID_VALUE = TO_STRING_BASE.toDouble().pow(TO_STRING_MAX_LENGTH.toDouble()).toLong()
    private fun forceToMaxLength(s: String): String {
      return "0".repeat(max(0, TO_STRING_MAX_LENGTH - s.length)) + s
    }

    @Throws(NumberFormatException::class)
    fun parse(string: String): TaskIdImpl {
      return TaskIdImpl(java.lang.Long.parseUnsignedLong(string, TO_STRING_BASE))
    }

    private fun log(value: Double, base: Double): Double {
      return ln(value) / ln(base)
    }

    fun generate(b: TypedContainer<TaskIdImpl>): TaskIdImpl {
      // TODO(vxi4873454w0): more sophisticated algorithm
      while (true) {
        val id = TaskIdImpl((Math.random() * MAX_ID_VALUE).toLong())
        if (!b.contains(id)) {
          return id
        }
      }
    }
  }
}