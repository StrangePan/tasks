package me.strangepan.tasks.engine.util.rx

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.Arrays
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.immutable.ImmutableSet
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ObservablesTest {
  @Test
  fun incrementingInteger_withNoParameters_take100_emitsSequentially() {
    val expected = arrayOfNulls<Int>(100)
    for (i in expected.indices) {
      expected[i] = i
    }
    Observables.incrementingInteger()
        .take(expected.size.toLong())
        .test()
        .assertValues(*expected)
  }

  @Test
  fun incrementingInteger_multipleSubscribers_doesNotShareState() {
    val expected = arrayOfNulls<Int>(100)
    for (i in expected.indices) {
      expected[i] = i
    }
    val underTest = Observables.incrementingInteger().take(expected.size.toLong())
    underTest.test().assertValues(*expected)
    underTest.test().assertValues(*expected)
  }

  @Test
  fun incrementingInteger_withIncrement_increments() {
    val expected = arrayOfNulls<Int>(100)
    for (i in expected.indices) {
      expected[i] = i * 7
    }
    Observables.incrementingInteger(0, 7).take(expected.size.toLong()).test().assertValues(*expected)
  }

  @Test
  fun incrementingInteger_withIncrement0_doesNotIncrement() {
    val expected = arrayOfNulls<Int>(100)
    Arrays.fill(expected, 0)
    Observables.incrementingInteger(0, 0).take(expected.size.toLong()).test().assertValues(*expected)
  }

  @Test
  fun incrementingInteger_withIncrementNegative_decrements() {
    val expected = arrayOfNulls<Int>(100)
    for (i in expected.indices) {
      expected[i] = -i
    }
    Observables.incrementingInteger(0, -1).take(expected.size.toLong()).test().assertValues(*expected)
  }

  @Test
  fun incrementingInteger_withStart_startsAtStart() {
    val expected = arrayOfNulls<Int>(100)
    for (i in expected.indices) {
      expected[i] = i + 93
    }
    Observables.incrementingInteger(93, 1).take(expected.size.toLong()).test().assertValues(*expected)
  }

  @Test
  fun incrementingLong_withNoParameters_take100_emitsSequentially() {
    val expected = arrayOfNulls<Long>(100)
    for (i in expected.indices) {
      expected[i] = i.toLong()
    }
    Observables.incrementingLong()
        .take(expected.size.toLong())
        .test()
        .assertValues(*expected)
  }

  @Test
  fun incrementingLong_multipleSubscribers_doesNotShareState() {
    val expected = arrayOfNulls<Long>(100)
    for (i in expected.indices) {
      expected[i] = i.toLong()
    }
    val underTest = Observables.incrementingLong().take(expected.size.toLong())
    underTest.test().assertValues(*expected)
    underTest.test().assertValues(*expected)
  }

  @Test
  fun incrementingLong_withIncrement_increments() {
    val expected = arrayOfNulls<Long>(100)
    for (i in expected.indices) {
      expected[i] = i.toLong() * 7
    }
    Observables.incrementingLong(0, 7).take(expected.size.toLong()).test().assertValues(*expected)
  }

  @Test
  fun incrementingLong_withIncrement0_doesNotIncrement() {
    val expected = arrayOfNulls<Long>(100)
    Arrays.fill(expected, 0L)
    Observables.incrementingLong(0, 0).take(expected.size.toLong()).test().assertValues(*expected)
  }

  @Test
  fun incrementingLong_withIncrementNegative_decrements() {
    val expected = arrayOfNulls<Long>(100)
    for (i in expected.indices) {
      expected[i] = (-i).toLong()
    }
    Observables.incrementingLong(0, -1).take(expected.size.toLong()).test().assertValues(*expected)
  }

  @Test
  fun incrementingLong_withStart_startsAtStart() {
    val expected = arrayOfNulls<Long>(100)
    for (i in expected.indices) {
      expected[i] = i.toLong() + 93
    }
    Observables.incrementingLong(93, 1).take(expected.size.toLong()).test().assertValues(*expected)
  }

  @Test
  fun toImmutableList_withItems_emitsItemsAsList() {
    Observable.just(1, 2, 3, 4, 5)
        .to(Observables.toImmutableList())
        .test()
        .assertValue(ImmutableList.of(1, 2, 3, 4, 5))
        .assertComplete()
  }

  @Test
  fun toImmutableList_whenEmpty_emitsEmptyList() {
    Observable.empty<Any>()
        .to(Observables.toImmutableList())
        .test()
        .assertValue(ImmutableList.empty())
        .assertComplete()
  }

  @Test
  fun toImmutableList_whenNeverCompletes_emitsNothing() {
    Observable.never<Any>()
        .to(Observables.toImmutableList())
        .test()
        .assertNoValues()
        .assertNotComplete()
  }

  @Test
  fun toImmutableList_whenHot_withLateSubscribers_doesNotShareState() {
    val source: Subject<Int> = PublishSubject.create()
    val underTest = source.to(Observables.toImmutableList())
    val firstObserver = underTest.test()
    source.onNext(0)
    source.onNext(1)
    source.onNext(2)
    val secondObserver = underTest.test()
    source.onNext(3)
    source.onNext(4)
    source.onNext(5)
    source.onComplete()
    firstObserver.assertValue(ImmutableList.of(0, 1, 2, 3, 4, 5)).assertComplete()
    secondObserver.assertValue(ImmutableList.of(3, 4, 5)).assertComplete()
    underTest.test().assertValue(ImmutableList.empty()).assertComplete()
  }

  @Test
  fun toImmutableMap_withItems_emitsItemsAsMap() {
    Observable.just(1, 2, 3, 4, 5)
        .to(Observables.toImmutableMap({ it }) { it })
        .test()
        .assertValue(
            ImmutableMap.builder<Int, Int>()
                .putMapping(1, 1)
                .putMapping(2, 2)
                .putMapping(3, 3)
                .putMapping(4, 4)
                .putMapping(5, 5)
                .build())
        .assertComplete()
  }

  @Test
  fun toImmutableMap_whenEmpty_emitsEmptyMap() {
    Observable.empty<Any>()
        .to(Observables.toImmutableMap({ it }) { it })
        .test()
        .assertValue(ImmutableMap.empty())
        .assertComplete()
  }

  @Test
  fun toImmutableMap_whenNeverCompletes_emitsNothing() {
    Observable.never<Any>()
        .to(Observables.toImmutableMap({ it }) { it })
        .test()
        .assertNoValues()
        .assertNotComplete()
  }

  @Test
  fun toImmutableMap_whenHot_withLateSubscribers_doesNotShareState() {
    val source: Subject<Int> = PublishSubject.create()
    val underTest = source.to(Observables.toImmutableMap<Int, Int, Int>({ it }, { it }))
    val firstObserver = underTest.test()
    source.onNext(0)
    source.onNext(1)
    source.onNext(2)
    val secondObserver = underTest.test()
    source.onNext(3)
    source.onNext(4)
    source.onNext(5)
    source.onComplete()
    firstObserver.assertValue(
        ImmutableMap.builder<Int, Int>()
            .putMapping(0, 0)
            .putMapping(1, 1)
            .putMapping(2, 2)
            .putMapping(3, 3)
            .putMapping(4, 4)
            .putMapping(5, 5)
            .build())
        .assertComplete()
    secondObserver.assertValue(
        ImmutableMap.builder<Int, Int>()
            .putMapping(3, 3)
            .putMapping(4, 4)
            .putMapping(5, 5)
            .build())
        .assertComplete()
    underTest.test().assertValue(ImmutableMap.empty()).assertComplete()
  }

  @Test
  fun toImmutableSet_withItems_emitsItemsAsSet() {
    Observable.just(1, 2, 3, 4, 5)
        .to(Observables.toImmutableSet())
        .test()
        .assertValue(ImmutableSet.of(1, 2, 3, 4, 5))
        .assertComplete()
  }

  @Test
  fun toImmutableSet_whenEmpty_emitsEmptySet() {
    Observable.empty<Any>()
        .to(Observables.toImmutableSet())
        .test()
        .assertValue(ImmutableSet.empty())
        .assertComplete()
  }

  @Test
  fun toImmutableSet_whenNeverCompletes_emitsNothing() {
    Observable.never<Any>()
        .to(Observables.toImmutableSet())
        .test()
        .assertNoValues()
        .assertNotComplete()
  }

  @Test
  fun toImmutableSet_whenHot_withLateSubscribers_doesNotShareState() {
    val source: Subject<Int> = PublishSubject.create()
    val underTest = source.to(Observables.toImmutableSet())
    val firstObserver = underTest.test()
    source.onNext(0)
    source.onNext(1)
    source.onNext(2)
    val secondObserver = underTest.test()
    source.onNext(3)
    source.onNext(4)
    source.onNext(5)
    source.onComplete()
    firstObserver.assertValue(ImmutableSet.of(0, 1, 2, 3, 4, 5)).assertComplete()
    secondObserver.assertValue(ImmutableSet.of(3, 4, 5)).assertComplete()
    underTest.test().assertValue(ImmutableSet.empty()).assertComplete()
  }
}