package tasks.cli.feature.graph

/** Model for parsed graph/xl command arguments.  */
class GraphArguments internal constructor(
    /**
     * Whether orn ot to list all tasks, including completed tasks. Defaults to `false`. Unless
     * set, completed tasks should be stripped from the output.
     */
    val isAllSet: Boolean)