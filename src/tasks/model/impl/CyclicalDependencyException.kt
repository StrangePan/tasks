package tasks.model.impl

import java.lang.System.lineSeparator
import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList

class CyclicalDependencyException internal constructor(msg: String, cycle: List<out Any>) : IllegalStateException(buildMessage(msg, ImmutableList.copyOf(cycle))) {
  val cycle: List<Any> = ImmutableList.copyOf(cycle)

  companion object {
    private const val serialVersionUID = -1193897049921304555L

    private fun buildMessage(prefix: String, cycle: List<*>): String {
      val message = StringBuilder().append(prefix).append(lineSeparator())
      message.append("->").append(cycle.itemAt(0)).append(lineSeparator())
      for (i in 1 until cycle.count()) {
        message.append("| ").append(cycle.itemAt(i)).append(lineSeparator())
      }
      message.append("--").append(cycle.itemAt(0))
      return message.toString()
    }
  }
}