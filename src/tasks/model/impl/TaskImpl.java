package tasks.model.impl;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;

import io.reactivex.Observable;
import java.util.Objects;
import java.util.Optional;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.immutable.ImmutableSet;
import omnia.data.structure.mutable.MutableSet;
import omnia.data.structure.mutable.TreeSet;
import tasks.model.Task;

final class TaskImpl implements Task {

  private final TaskStoreImpl store;
  private final TaskId id;
  private final TaskData data;

  private final Memoized<ImmutableSet<TaskImpl>> blockingTasks;
  private final Memoized<ImmutableSet<TaskImpl>> blockedTasks;
  private final Memoized<ImmutableSet<TaskImpl>> openBlockingTasks;

  TaskImpl(TaskStoreImpl store, TaskId id, TaskData data) {
    this.store = requireNonNull(store);
    this.id = requireNonNull(id);
    this.data = requireNonNull(data);

    blockingTasks = memoize(() -> store.allTasksBlocking(id));
    blockedTasks = memoize(() -> store.allTasksBlockedBy(id));
    openBlockingTasks = memoize(() -> store.allOpenTasksBlocking(id));
  }

  TaskId id() {
    return id;
  }

  @Override
  public String label() {
    return data.label();
  }

  @Override
  public boolean isCompleted() {
    return data.isCompleted();
  }

  @Override
  public boolean isUnblocked() {
    return !openBlockingTasks.value().isPopulated();
  }

  @Override
  public ImmutableSet<TaskImpl> blockingTasks() {
    return blockingTasks.value();
  }

  @Override
  public ImmutableSet<TaskImpl> blockedTasks() {
    return blockedTasks.value();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this
        || (obj instanceof TaskImpl
            && Objects.equals(id, ((TaskImpl) obj).id)
            && Objects.equals(store, ((TaskImpl) obj).store)
            && Objects.equals(data, ((TaskImpl) obj).data));
  }

  @Override
  public int hashCode() {
    return Objects.hash(store, id, data);
  }

  @Override
  public String toString() {
    return render().toString();
  }

  @Override
  public Output render() {
    String stringId = id.toString();
    TreeSet<String> allIds =
        Observable.fromIterable(store.allTasks())
            .cast(TaskImpl.class)
            .map(TaskImpl::id)
            .map(Object::toString)
            .collectInto(TreeSet.create(String::compareTo), MutableSet::add)
            .blockingGet();
    Optional<String> precedingId = allIds.itemPreceding(stringId);
    Optional<String> followingId = allIds.itemFollowing(stringId);

    int longestCommonPrefix = Math.max(
        precedingId.map(other -> longestCommonPrefix(other, stringId)).orElse(0),
        followingId.map(other -> longestCommonPrefix(other, stringId)).orElse(0)) + 1;
    return Output.builder()
        .underlined()
        .color(Output.Color16.LIGHT_GREEN)
        .append(stringId.substring(0, longestCommonPrefix))
        .defaultUnderline()
        .append(stringId.substring(longestCommonPrefix))
        .defaultColor()
        .append(
            isCompleted()
                ? Output.builder()
                    .color(Output.Color16.LIGHT_CYAN)
                    .append(" (completed)")
                    .build()
                : Output.empty())
        .append(": ")
        .defaultColor()
        .append(label())
        .build();
  }

  private static int longestCommonPrefix(String a, String b) {
    for (int i = 0;; i++) {
      if (i > a.length() || i > b.length() || a.charAt(i) != b.charAt(i)) {
        return i;
      }
    }
  }
}
