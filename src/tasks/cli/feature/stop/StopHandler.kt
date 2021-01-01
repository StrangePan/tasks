package tasks.cli.feature.stop

import omnia.data.cache.Memoized
import tasks.cli.command.common.simple.SimpleHandler
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskMutator

/** Business logic for the Stop command.  */
class StopHandler(taskStore: Memoized<out ObservableTaskStore>)
  : SimpleHandler<StopArguments>(
    taskStore,
    TaskMutator::stop,
    Comparator.comparing(Task::status),
    "task(s) stopped:",
    "task(s) already stopped:")