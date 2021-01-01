package tasks.cli.feature.start

import omnia.data.cache.Memoized
import tasks.cli.command.common.simple.SimpleHandler
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskMutator

/** Business logic for the Start command.  */
class StartHandler(taskStore: Memoized<out ObservableTaskStore>)
  : SimpleHandler<StartArguments>(
    taskStore,
    TaskMutator::start,
    Comparator.comparing(Task::status),
    "task(s) started:",
    "task(s) already started:")