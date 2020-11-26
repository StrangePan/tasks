package tasks.cli.feature.list;

/** Model for parsed List command arguments. */
public final class ListArguments {
  private final boolean isUnblockedSet;
  private final boolean isBlockedSet;
  private final boolean isCompletedSet;
  private final boolean isStartedSet;

  ListArguments(
      boolean isUnblockedSet, boolean isBlockedSet, boolean isCompletedSet, boolean isStartedSet) {
    this.isUnblockedSet = isUnblockedSet;
    this.isBlockedSet = isBlockedSet;
    this.isCompletedSet = isCompletedSet;
    this.isStartedSet = isStartedSet;
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

  /**
   * Whether or not to filter the result so that only started tasks are listed. If this flag is set,
   * then only started tasks should be included in the output. Defaults to false.
   *
   * <p>Unlike other flags, this one is subtractive. Setting this flag can always reduce the
   * output.</p>
   */
  public boolean isStartedSet() {
    return isStartedSet;
  }
}
