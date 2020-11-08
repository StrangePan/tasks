package tasks.util.rx;

import static tasks.util.rx.Unit.unit;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import org.reactivestreams.Publisher;

public final class Flowables {

  private Flowables() {}

  public static <T> FlowableTransformer<Flowable<T>, Flowable<T>> takeUntil(Completable completable) {
    return flowable -> flowable.takeUntil(completable.andThen(Flowable.just(unit())));
  }

  public static Publisher<Unit> completableCompletes(Completable completable) {
    return completable.andThen(Flowable.just(unit()));
  }
}
