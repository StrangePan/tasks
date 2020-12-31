package tasks.model;

import java.util.Optional;
import omnia.data.structure.immutable.ImmutableDirectedGraph;
import omnia.data.structure.immutable.ImmutableSet;

public interface TaskStore {

  Optional<? extends Task> lookUpById(long id);

  Optional<? extends Task> lookUpById(TaskId id);

  ImmutableSet<? extends Task> allTasks();

  ImmutableSet<? extends Task> allOpenTasksWithoutOpenBlockers();

  ImmutableSet<? extends Task> allOpenTasksWithOpenBlockers();

  ImmutableSet<? extends Task> allCompletedTasks();

  ImmutableSet<? extends Task> allOpenTasks();

  ImmutableSet<? extends Task> allTasksMatchingCliPrefix(String prefix);

  ImmutableDirectedGraph<? extends Task> taskGraph();
}
