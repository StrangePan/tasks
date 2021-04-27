package me.strangepan.tasks.cli.feature.start

import omnia.data.cache.Memoized
import me.strangepan.tasks.cli.command.common.simple.SimpleHandler
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.model.TaskMutator

/** Business logic for the Start command.  */
class StartHandler(taskStore: Memoized<out ObservableTaskStore>)
  : SimpleHandler<StartArguments>(
    taskStore,
    TaskMutator::start,
    Comparator.comparing(Task::status),
    "task(s) started:",
    "task(s) already started:")