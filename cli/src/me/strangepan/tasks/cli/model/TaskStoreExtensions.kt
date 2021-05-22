package me.strangepan.tasks.cli.model

import java.util.Objects
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskId
import me.strangepan.tasks.engine.model.TaskStore
import omnia.data.stream.Collectors
import omnia.data.structure.immutable.ImmutableSet


/** Retrieves all tasks whose string representation of [TaskId] starts with [prefix]. */
fun TaskStore.allTasksMatchingCliPrefix(prefix: String): ImmutableSet<out Task> {
    Objects.requireNonNull(prefix)
    return allTasks()
      .stream()
      .filter { it.id.toString().regionMatches(0, prefix, 0, prefix.length) }
      .collect(Collectors.toImmutableSet())
}
