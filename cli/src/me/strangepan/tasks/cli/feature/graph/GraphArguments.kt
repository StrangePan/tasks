package me.strangepan.tasks.cli.feature.graph

import omnia.data.structure.List
import me.strangepan.tasks.engine.model.Task

/** Model for parsed graph/xl command arguments.  */
class GraphArguments internal constructor(
  /**
   * Whether or not to list all me.strangepan.tasks.engine.tasks, including completed me.strangepan.tasks.engine.tasks. Defaults to `false`. Unless
   * set, completed me.strangepan.tasks.engine.tasks should be stripped from the output.
   */
  val isAllSet: Boolean,

  /**
   * An optional set of me.strangepan.tasks.engine.tasks to specifically target when printing. Only the subgraphs containing
   * these me.strangepan.tasks.engine.tasks should be printed.
   */
  val tasksToRelateTo: List<Task>,

  /**
   * An optional set of me.strangepan.tasks.engine.tasks to specifically target when printing. Only the me.strangepan.tasks.engine.tasks blocking these
   * me.strangepan.tasks.engine.tasks should be printed.
   */
  val tasksToGetBlockersOf: List<Task>,
)