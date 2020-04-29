package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import omnia.data.structure.Set;
import omnia.data.structure.mutable.MutableSet;
import omnia.data.structure.mutable.TreeSet;
import tasks.model.Task;
import tasks.model.TaskMutator;

final class TaskImpl implements Task {

  private final TaskStoreImpl store;
  private final TaskId id;

  TaskImpl(TaskStoreImpl store, TaskId id) {
    this.store = requireNonNull(store);
    this.id = requireNonNull(id);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TaskImpl
        && ((TaskImpl) other).store.equals(store)
        && ((TaskImpl) other).id.equals(id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(store, id);
  }

  @Override
  public String toString() {
    String stringId = id.toString();
    TreeSet<String> allIds =
        store.allTasks()
            .firstOrError()
            .flatMapObservable(Observable::fromIterable)
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
    String formattingResetCode = "\033[0m";
    String underlineInitCode = "\033[4m";
    String formattedStringId =
        underlineInitCode
            + stringId.substring(0, longestCommonPrefix)
            + formattingResetCode
            + stringId.substring(longestCommonPrefix);

    return new StringBuilder()
        .append(formattedStringId)
        .append(isCompleted().blockingFirst() ? " (completed)" : "")
        .append(": ")
        .append(label().blockingFirst())
        .toString();
  }

  private static int longestCommonPrefix(String a, String b) {
    for (int i = 0;; i++) {
      if (i > a.length() || i > b.length() || a.charAt(i) != b.charAt(i)) {
        return i;
      }
    }
  }

  @Override
  public Flowable<Boolean> isCompleted() {
    return store().lookUp(id)
        .toSingle()
        .flatMapPublisher(f -> f)
        .map(TaskData::isCompleted);
  }

  @Override
  public Flowable<String> label() {
    return store().lookUp(id)
        .toSingle()
        .flatMapPublisher(f -> f)
        .map(TaskData::label);
  }

  @Override
  public Query query() {
    return new Query() {

      @Override
      public Flowable<Set<Task>> tasksBlockedByThis() {
        return store().allTasksBlockedBy(TaskImpl.this);
      }

      @Override
      public Flowable<Set<Task>> tasksBlockingThis() {
        return store().allTasksBlocking(TaskImpl.this);
      }
    };
  }

  @Override
  public Completable mutate(Function<? super TaskMutator, ? extends TaskMutator> mutator) {
    return store().mutateTask(this, mutator);
  }

  TaskStoreImpl store() {
    return store;
  }

  TaskId id() {
    return id;
  }
}
