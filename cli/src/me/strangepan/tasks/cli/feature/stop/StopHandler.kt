package me.strangepan.tasks.cli.feature.stop

import omnia.data.cache.Memoized
import me.strangepan.tasks.cli.command.common.simple.SimpleHandler
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskMutator

/** Business logic for the Stop command.  */
class StopHandler(taskStore: Memoized<out ObservableTaskStore>)
  : SimpleHandler<StopArguments>(
    taskStore,
    TaskMutator::stop,
    Comparator.comparing(Task::status),
    "task(s) stopped:",
    "task(s) already stopped:")