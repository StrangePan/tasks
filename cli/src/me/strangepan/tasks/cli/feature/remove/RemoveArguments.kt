package me.strangepan.tasks.cli.feature.remove

import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleArguments
import me.strangepan.tasks.engine.model.Task

/** Model for parsed Remove command arguments.  */
class RemoveArguments internal constructor(tasks: List<Task>, private val force: Boolean) : SimpleArguments(tasks) {
  /** Force. auto-confirm deletions, bypassing manual confirmations.  */
  fun force(): Boolean {
    return force
  }
}