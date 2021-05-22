package me.strangepan.tasks.cli.model

import kotlin.math.max
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskId
import omnia.cli.out.Output
import omnia.data.structure.SortedSet

/** Extensions for [Task] models. */

fun Task.render(): Output {
  val allIds: SortedSet<out TaskId> = store.allTaskIds()
  val precedingId = allIds.itemPrecedingUnknownTyped(id)
  val followingId = allIds.itemFollowingUnknownTyped(id)
  val stringId = id.toString()
  val longestCommonPrefix = max(
    precedingId?.toString()?.longestCommonPrefix(stringId)?:0,
    followingId?.toString()?.longestCommonPrefix(stringId)?:0) + 1
  return Output.builder()
    .underlined()
    .color(Output.Color16.LIGHT_GREEN)
    .append(stringId.substring(0, longestCommonPrefix))
    .defaultUnderline()
    .append(stringId.substring(longestCommonPrefix))
    .defaultColor()
    .append(status.render())
    .append(": ")
    .defaultColor()
    .append(label)
    .build()
}

private fun String.longestCommonPrefix(other: String): Int {
  var i = 0
  while (true) {
    if (i > this.length || i > other.length || this[i] != other[i]) {
      return i
    }
    i++
  }
}

private fun Task.Status.render(): Output {
  return when (this) {
    Task.Status.STARTED -> Output.builder()
      .color(Output.Color16.YELLOW)
      .append(" (started)")
      .build()
    Task.Status.COMPLETED -> Output.builder()
      .color(Output.Color16.LIGHT_CYAN)
      .append(" (completed)")
      .build()
    else -> Output.empty()
  }
}