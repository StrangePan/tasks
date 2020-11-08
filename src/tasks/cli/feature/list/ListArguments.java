package tasks.cli.feature.list;

/** Model for parsed List command arguments. */
public final class ListArguments {
  private final boolean isUnblockedSet;
  private final boolean isBlockedSet;
  private final boolean isCompletedSet;

  ListArguments(boolean isUnblockedSet, boolean isBlockedSet, boolean isCompletedSet) {
    this.isUnblockedSet = isUnblockedSet;
    this.isBlockedSet = isBlockedSet;
    this.isCompletedSet = isCompletedSet;
  }

  /**
   * Whether or not to list all unblocked tasks. Defaults to {@code true} if no other flags are
   * set.
   */
  public boolean isUnblockedSet() {
    return isUnblockedSet;
  }

  /**
   * Whether or not to list all blocked tasks. Defaults to {@code false} unless set by the user
   * specifically or via the {@link ListCommand#ALL_OPTION} flag.
   */
  public boolean isBlockedSet() {
    return isBlockedSet;
  }

  /**
   * Whether or not to list all completed tasks. Defaults to {@code false} unless set by the user
   * specifically or via the {@link ListCommand#ALL_OPTION} flag.
   */
  public boolean isCompletedSet() {
    return isCompletedSet;
  }
}
