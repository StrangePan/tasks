package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.Objects;
import java.util.function.Function;
import omnia.data.structure.Set;
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
        return store().tasksBlockedBy(TaskImpl.this);
      }

      @Override
      public Flowable<Set<Task>> tasksBlockingThis() {
        return store().tasksBlocking(TaskImpl.this);
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
