package tasks.cli.feature.complete

import omnia.data.cache.Memoized
import tasks.cli.command.common.simple.SimpleHandler
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskMutator

/** Business logic for the Complete command.  */
class CompleteHandler(val taskStore: Memoized<out ObservableTaskStore>)
  : SimpleHandler<CompleteArguments>(
    taskStore,
    TaskMutator::complete,
    Comparator.comparing(Task::status),
    "task(s) completed:",
    "task(s) already completed:")