package tasks.util.rx

import io.reactivex.rxjava3.core.Emitter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableConverter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.functions.Supplier
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.immutable.ImmutableSet

object Observables {
  @JvmStatic
  @JvmOverloads
  fun incrementingInteger(start: Int = 0, increment: Int = 1): Observable<Int> {
    return Observable.generate(
        Supplier { start },
        BiFunction { i: Int, emitter: Emitter<Int> ->
          emitter.onNext(i)
          i + increment
        })
  }

  @JvmStatic
  @JvmOverloads
  fun incrementingLong(start: Long = 0, increment: Long = 1): Observable<Long> {
    return Observable.generate(
        Supplier { start },
        BiFunction { i: Long, emitter: Emitter<Long> ->
          emitter.onNext(i)
          i + increment
        })
  }

  @JvmStatic
  fun <T> toImmutableList(): ObservableConverter<T, Single<ImmutableList<T>>> {
    return ObservableConverter { observable: Observable<T> ->
      observable.collect({ ImmutableList.builder<T>() }, { builder, item -> builder.add(item) })
          .map { builder -> builder.build() }
    }
  }

  @JvmStatic
  fun <T, K, V> toImmutableMap(
      keyExtractor: Function<in T, out K>,
      valueExtractor: Function<in T, out V>): ObservableConverter<T, Single<ImmutableMap<K, V>>> {
    return ObservableConverter { observable: Observable<T> ->
      observable.collect({ ImmutableMap.builder<K, V>() }, { builder, item -> builder.putMapping(keyExtractor.apply(item), valueExtractor.apply(item)) })
          .map { builder -> builder.build() }
    }
  }

  @JvmStatic
  fun <T> toImmutableSet(): ObservableConverter<T, Single<ImmutableSet<T>>> {
    return ObservableConverter { observable: Observable<T> ->
      observable.collect({ ImmutableSet.builder<T>() }, { builder, item -> builder.add(item) })
          .map { builder -> builder.build() }
    }
  }
}