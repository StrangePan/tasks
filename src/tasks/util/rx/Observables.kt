package tasks.util.rx;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableConverter;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
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

  public static <T> ObservableConverter<T, Single<ImmutableList<T>>> toImmutableList() {
    return observable ->
        observable.<ImmutableList.Builder<T>>collect(
                ImmutableList::builder, ImmutableList.Builder::add)
            .map(ImmutableList.Builder::build);
  }

  public static <T, K, V> ObservableConverter<T, Single<ImmutableMap<K, V>>> toImmutableMap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor) {
    return observable ->
        observable.<ImmutableMap.Builder<K, V>>collect(
            ImmutableMap::builder,
            (builder, item) ->
                builder.putMapping(keyExtractor.apply(item), valueExtractor.apply(item)))
            .map(ImmutableMap.Builder::build);
  }

  public static <T> ObservableConverter<T, Single<ImmutableSet<T>>> toImmutableSet() {
    return observable ->
        observable.<ImmutableSet.Builder<T>>collect(
                ImmutableSet::builder, ImmutableSet.Builder::add)
            .map(ImmutableSet.Builder::build);
  }
}
