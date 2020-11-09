package tasks.model;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.function.Function;
import omnia.cli.out.Output;
import omnia.data.structure.Set;

/**
 * Represents a single task of any type, including all the data necessary to render it to the user.
 *
 *
 * Some fields of a ObservableTask are volatile and are thus observable, forcing callers to react to changes
 * in the field's value. Not all fields of a task must be loaded into memory in order to implement
 * this interface; individual fields can be lazily loaded when required or fetched from slow
 * resource such as disk or the network.
 */
public interface ObservableTask {

  Flowable<Boolean> isCompleted();

  Flowable<Boolean> isUnblocked();

  Flowable<String> label();

  Query query();

  interface Query {

    Flowable<Set<ObservableTask>> tasksBlockedByThis();

    Flowable<Set<ObservableTask>> tasksBlockingThis();
  }

  Completable mutate(Function<? super TaskMutator, ? extends TaskMutator> mutator);

  Output render();
}
