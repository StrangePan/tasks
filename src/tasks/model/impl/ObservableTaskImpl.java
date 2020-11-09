package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import omnia.cli.out.Output;
import omnia.data.structure.Set;
import omnia.data.structure.mutable.MutableSet;
import omnia.data.structure.mutable.TreeSet;
import tasks.model.ObservableTask;
import tasks.model.TaskMutator;

final class ObservableTaskImpl implements ObservableTask {

  private final ObservableTaskStoreImpl store;
  private final TaskId id;

  ObservableTaskImpl(ObservableTaskStoreImpl store, TaskId id) {
    this.store = requireNonNull(store);
    this.id = requireNonNull(id);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ObservableTaskImpl
        && ((ObservableTaskImpl) other).store.equals(store)
        && ((ObservableTaskImpl) other).id.equals(id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(store, id);
  }

  @Override
  public String toString() {
    return render().toString();
  }

  @Override
  public Output render() {
    String stringId = id.toString();
    TreeSet<String> allIds =
        store.allTasks()
            .firstOrError()
            .flatMapObservable(Observable::fromIterable)
            .cast(ObservableTaskImpl.class)
            .map(ObservableTaskImpl::id)
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
            isCompleted().blockingFirst()
                ? Output.builder()
                .color(Output.Color16.LIGHT_CYAN)
                .append(" (completed)")
                .build()
                : Output.empty())
        .append(": ")
        .defaultColor()
        .append(label().blockingFirst())
        .build();
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
    return store()
        .lookUp(id)
        .takeWhile(Optional::isPresent)
        .map(Optional::get)
        .map(TaskData::isCompleted);
  }

  @Override
  public Flowable<Boolean> isUnblocked() {
    return query().tasksBlockingThis()
        .flatMapSingle(
            tasks ->
                Observable.fromIterable(tasks)
                    .flatMapSingle(task -> task.isCompleted().firstOrError())
                    .map(isCompleted -> !isCompleted)
                    .filter(isOpen -> isOpen)
                    .first(/*or else*/ false))
        .map(atLeastOneBlockingTaskIsOpen -> !atLeastOneBlockingTaskIsOpen);
  }

  @Override
  public Flowable<String> label() {
    return store()
        .lookUp(id)
        .takeWhile(Optional::isPresent)
        .map(Optional::get)
        .map(TaskData::label);
  }

  @Override
  public Query query() {
    return new Query() {

      @Override
      public Flowable<Set<ObservableTask>> tasksBlockedByThis() {
        return store().allTasksBlockedBy(ObservableTaskImpl.this);
      }

      @Override
      public Flowable<Set<ObservableTask>> tasksBlockingThis() {
        return store().allTasksBlocking(ObservableTaskImpl.this);
      }
    };
  }

  @Override
  public Completable mutate(Function<? super TaskMutator, ? extends TaskMutator> mutator) {
    return store().mutateTask(this, mutator);
  }

  ObservableTaskStoreImpl store() {
    return store;
  }

  TaskId id() {
    return id;
  }
}
