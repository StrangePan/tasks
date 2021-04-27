package me.strangepan.tasks.cli.feature.reopen

import omnia.data.cache.Memoized
import me.strangepan.tasks.cli.command.common.simple.SimpleHandler
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskMutator

/** Business logic for the Reopen command.  */
class ReopenHandler(taskStore: Memoized<out ObservableTaskStore>)
  : SimpleHandler<ReopenArguments>(
    taskStore,
    TaskMutator::reopen,
    Comparator.comparing(Task::status),
    "task(s) reopened:",
    "task(s) already open:")