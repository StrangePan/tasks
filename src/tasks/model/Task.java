package tasks.model;

import io.reactivex.Flowable;
import omnia.data.structure.observable.ObservableSet;

/**
 * Represents a single task of any type, including all the data necessary to render it to the user.
 *
 *
 * Some fields of a Task are volatile and are thus observable, forcing callers to react to changes
 * in the field's value. Not all fields of a task must be loaded into memory in order to implement
 * this interface; individual fields can be lazily loaded when required or fetched from slow
 * resource such as disk or the network.
 */
public interface Task {

  Flowable<String> label();

  Query query();

  interface Query {

    ObservableSet<Task> tasksBlockedByThis();

    ObservableSet<Task> tasksBlockingThis();
  }
}
