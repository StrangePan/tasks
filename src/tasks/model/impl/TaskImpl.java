package tasks.model.impl;

import static java.util.Objects.requireNonNull;

import io.reactivex.Flowable;
import java.util.Objects;
import omnia.data.structure.observable.ObservableSet;
import tasks.model.Task;

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
    return store.lookUp(id)
        .toSingle()
        .flatMapPublisher(f -> f)
        .map(TaskData::label);
  }

  @Override
  public Query query() {
    return new Query() {

      @Override
      public ObservableSet<Task> tasksBlockedByThis() {
        return store.tasksBlockedBy(TaskImpl.this);
      }

      @Override
      public ObservableSet<Task> tasksBlockingThis() {
        return store.tasksBlocking(TaskImpl.this);
      }
    };
  }

  TaskStoreImpl store() {
    return store;
  }

  TaskId id() {
    return id;
  }
}
