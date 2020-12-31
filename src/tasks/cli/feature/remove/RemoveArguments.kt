package tasks.cli.feature.remove;

import omnia.data.structure.List;
import tasks.cli.command.common.simple.SimpleArguments;
import tasks.model.Task;

/** Model for parsed Remove command arguments. */
public final class RemoveArguments extends SimpleArguments {

  private final boolean force;

  RemoveArguments(List<Task> tasks, boolean force) {
    super(tasks);
    this.force = force;
  }

  /** The tasks to remove from the store. */
  @Override
  public List<Task> tasks() {
    return super.tasks();
  }

  /** Force. auto-confirm deletions, bypassing manual confirmations. */
  public boolean force() {
    return force;
  }
}
