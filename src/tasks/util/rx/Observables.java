package tasks.util.rx;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
public final class Observables {

  private Observables() {}

  public static Observable<Integer> incrementingInteger() {
    return incrementingInteger(0, 1);
  }

  public static Observable<Integer> incrementingInteger(int start, int increment) {
    return Observable.generate(
        () -> start,
        (i, emitter) -> {
          emitter.onNext(i);
          return i + increment;
        });
  }

  public static Observable<Long> incrementingLong() {
    return incrementingLong(0, 1);
  }

  public static Observable<Long> incrementingLong(long start, long increment) {
    return Observable.generate(
        () -> start,
        (i, emitter) -> {
          emitter.onNext(i);
          return i + increment;
        });
  }

  public static <T> Function<Observable<? extends T>, Single<ImmutableList<T>>> toImmutableList() {
    return observable ->
        observable.collect(ImmutableList::<T>builder, ImmutableList.Builder::add)
            .map(ImmutableList.Builder::build);
  }

  public static <T, K, V> Function<Observable<T>, Single<ImmutableMap<K, V>>> toImmutableMap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor) {
    return observable ->
        observable.collect(
            ImmutableMap::<K, V>builder,
            (builder, item) ->
                builder.putMapping(keyExtractor.apply(item), valueExtractor.apply(item)))
            .map(ImmutableMap.Builder::build);
  }

  public static <T> Function<Observable<T>, Single<ImmutableSet<T>>> toImmutableSet() {
    return observable ->
        observable.collect(ImmutableSet::<T>builder, ImmutableSet.Builder::add)
            .map(ImmutableSet.Builder::build);
  }
}
