package me.strangepan.tasks.cli.feature.complete

import omnia.data.cache.Memoized
import me.strangepan.tasks.cli.command.common.simple.SimpleHandler
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskMutator

/** Business logic for the Complete command.  */
class CompleteHandler(val taskStore: Memoized<out ObservableTaskStore>)
  : SimpleHandler<CompleteArguments>(
    taskStore,
    TaskMutator::complete,
    Comparator.comparing(Task::status),
    "task(s) completed:",
    "task(s) already completed:")