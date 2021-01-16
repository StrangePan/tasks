package tasks.cli.feature.graph

import omnia.data.structure.List
import tasks.model.Task

/** Model for parsed graph/xl command arguments.  */
class GraphArguments internal constructor(
  /**
   * Whether or not to list all tasks, including completed tasks. Defaults to `false`. Unless
   * set, completed tasks should be stripped from the output.
   */
  val isAllSet: Boolean,

  /**
   * An optional set of tasks to specifically target when printing. Only the subgraphs containing
   * these tasks should be printed.
   */
  val tasksToRelateTo: List<Task>,

  /**
   * An optional set of tasks to specifically target when printing. Only the tasks blocking these
   * tasks should be printed.
   */
  val tasksToGetBlockersOf: List<Task>,
)