package tasks.cli.command.list;

public final class ListArguments {
  private final boolean isUnblockedSet;
  private final boolean isBlockedSet;
  private final boolean isCompletedSet;

  ListArguments(boolean isUnblockedSet, boolean isBlockedSet, boolean isCompletedSet) {
    this.isUnblockedSet = isUnblockedSet;
    this.isBlockedSet = isBlockedSet;
    this.isCompletedSet = isCompletedSet;
  }

  public boolean isUnblockedSet() {
    return isUnblockedSet;
  }

  public boolean isBlockedSet() {
    return isBlockedSet;
  }

  public boolean isCompletedSet() {
    return isCompletedSet;
  }
}
