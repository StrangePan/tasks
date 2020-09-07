package tasks.cli.command.graph;

/** Model for parsed graph/xl command arguments. */
public final class GraphArguments {
  private final boolean isCompletedSet;
  private final boolean isUncompletedSet;

  GraphArguments(boolean isCompletedSet, boolean isUncompletedSet) {
    this.isCompletedSet = isCompletedSet;
    this.isUncompletedSet = isUncompletedSet;
  }

  /** Whether or not to list completed tasks. Defaults to {@code false}. */
  public boolean isCompletedSet() {
    return isCompletedSet;
  }

  /** Whether or not to list uncompleted tasks. Defaults to {@code true}. */
  public boolean isUncompletedSet() {
    return isUncompletedSet;
  }
}
