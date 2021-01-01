package tasks.cli.feature.reopen

import omnia.data.cache.Memoized
import tasks.cli.command.common.simple.SimpleHandler
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskMutator

/** Business logic for the Reopen command.  */
class ReopenHandler(taskStore: Memoized<out ObservableTaskStore>)
  : SimpleHandler<ReopenArguments>(
    taskStore,
    TaskMutator::reopen,
    Comparator.comparing(Task::status),
    "task(s) reopened:",
    "task(s) already open:")