package tasks.cli.command.graph;

/** Model for parsed graph/xl command arguments. */
public final class GraphArguments {
  private final boolean isAllSet;

  GraphArguments(boolean isAllSet) {
    this.isAllSet = isAllSet;
  }

  /**
   * Whether orn ot to list all tasks, including completed tasks. Defaults to {@code false}. Unless
   * set, completed tasks should be stripped from the output.
   */
  public boolean isAllSet() {
    return isAllSet;
  }
}
