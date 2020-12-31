package tasks.util.rx;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.Arrays;
import omnia.data.structure.immutable.ImmutableList;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.immutable.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ObservablesTest {

  @Test
  public void incrementingInteger_withNoParameters_take100_emitsSequentially() {
    Integer[] expected = new Integer[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = i;
    }

    Observables.incrementingInteger()
        .take(expected.length)
        .test()
        .assertValues(expected);
  }

  @Test
  public void incrementingInteger_multipleSubscribers_doesNotShareState() {
    Integer[] expected = new Integer[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = i;
    }

    Observable<Integer> underTest = Observables.incrementingInteger().take(expected.length);

    underTest.test().assertValues(expected);
    underTest.test().assertValues(expected);
  }

  @Test
  public void incrementingInteger_withIncrement_increments() {
    Integer[] expected = new Integer[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = i * 7;
    }

    Observables.incrementingInteger(0, 7).take(expected.length).test().assertValues(expected);
  }

  @Test
  public void incrementingInteger_withIncrement0_doesNotIncrement() {
    Integer[] expected = new Integer[100];
    Arrays.fill(expected, 0);

    Observables.incrementingInteger(0, 0).take(expected.length).test().assertValues(expected);
  }

  @Test
  public void incrementingInteger_withIncrementNegative_decrements() {
    Integer[] expected = new Integer[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = -i;
    }

    Observables.incrementingInteger(0, -1).take(expected.length).test().assertValues(expected);
  }

  @Test
  public void incrementingInteger_withStart_startsAtStart() {
    Integer[] expected = new Integer[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = i + 93;
    }

    Observables.incrementingInteger(93, 1).take(expected.length).test().assertValues(expected);
  }

  @Test
  public void incrementingLong_withNoParameters_take100_emitsSequentially() {
    Long[] expected = new Long[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = (long) i;
    }

    Observables.incrementingLong()
        .take(expected.length)
        .test()
        .assertValues(expected);
  }

  @Test
  public void incrementingLong_multipleSubscribers_doesNotShareState() {
    Long[] expected = new Long[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = (long) i;
    }

    Observable<Long> underTest = Observables.incrementingLong().take(expected.length);

    underTest.test().assertValues(expected);
    underTest.test().assertValues(expected);
  }

  @Test
  public void incrementingLong_withIncrement_increments() {
    Long[] expected = new Long[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = (long) i * 7;
    }

    Observables.incrementingLong(0, 7).take(expected.length).test().assertValues(expected);
  }

  @Test
  public void incrementingLong_withIncrement0_doesNotIncrement() {
    Long[] expected = new Long[100];
    Arrays.fill(expected, 0L);

    Observables.incrementingLong(0, 0).take(expected.length).test().assertValues(expected);
  }

  @Test
  public void incrementingLong_withIncrementNegative_decrements() {
    Long[] expected = new Long[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = (long) -i;
    }

    Observables.incrementingLong(0, -1).take(expected.length).test().assertValues(expected);
  }

  @Test
  public void incrementingLong_withStart_startsAtStart() {
    Long[] expected = new Long[100];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = (long) i + 93;
    }

    Observables.incrementingLong(93, 1).take(expected.length).test().assertValues(expected);
  }


  @Test
  public void toImmutableList_withItems_emitsItemsAsList() {
    Observable.just(1, 2, 3, 4, 5)
        .to(Observables.toImmutableList())
        .test()
        .assertValue(ImmutableList.of(1, 2, 3, 4, 5))
        .assertComplete();
  }

  @Test
  public void toImmutableList_whenEmpty_emitsEmptyList() {
    Observable.empty()
        .to(Observables.toImmutableList())
        .test()
        .assertValue(ImmutableList.empty())
        .assertComplete();
  }

  @Test
  public void toImmutableList_whenNeverCompletes_emitsNothing() {
    Observable.never()
        .to(Observables.toImmutableList())
        .test()
        .assertNoValues()
        .assertNotComplete();
  }

  @Test
  public void toImmutableList_whenHot_withLateSubscribers_doesNotShareState() {
    Subject<Integer> source = PublishSubject.create();

    Single<ImmutableList<Integer>> underTest = source.to(Observables.toImmutableList());

    TestObserver<ImmutableList<Integer>> firstObserver = underTest.test();

    source.onNext(0);
    source.onNext(1);
    source.onNext(2);

    TestObserver<ImmutableList<Integer>> secondObserver = underTest.test();

    source.onNext(3);
    source.onNext(4);
    source.onNext(5);

    source.onComplete();

    firstObserver.assertValue(ImmutableList.of(0, 1, 2, 3, 4, 5)).assertComplete();
    secondObserver.assertValue(ImmutableList.of(3, 4, 5)).assertComplete();
    underTest.test().assertValue(ImmutableList.empty()).assertComplete();
  }

  @Test
  public void toImmutableMap_withItems_emitsItemsAsMap() {
    Observable.just(1, 2, 3, 4, 5)
        .to(Observables.toImmutableMap(i -> i, i -> i))
        .test()
        .assertValue(
            ImmutableMap.<Integer, Integer>builder()
                .putMapping(1, 1)
                .putMapping(2, 2)
                .putMapping(3, 3)
                .putMapping(4, 4)
                .putMapping(5, 5)
                .build())
        .assertComplete();
  }

  @Test
  public void toImmutableMap_whenEmpty_emitsEmptyMap() {
    Observable.empty()
        .to(Observables.toImmutableMap(i -> i, i -> i))
        .test()
        .assertValue(ImmutableMap.empty())
        .assertComplete();
  }

  @Test
  public void toImmutableMap_whenNeverCompletes_emitsNothing() {
    Observable.never()
        .to(Observables.toImmutableMap(i -> i, i -> i))
        .test()
        .assertNoValues()
        .assertNotComplete();
  }

  @Test
  public void toImmutableMap_whenHot_withLateSubscribers_doesNotShareState() {
    Subject<Integer> source = PublishSubject.create();

    Single<ImmutableMap<Integer, Integer>> underTest = source.to(Observables.toImmutableMap(i -> i, i -> i));

    TestObserver<ImmutableMap<Integer, Integer>> firstObserver = underTest.test();

    source.onNext(0);
    source.onNext(1);
    source.onNext(2);

    TestObserver<ImmutableMap<Integer, Integer>> secondObserver = underTest.test();

    source.onNext(3);
    source.onNext(4);
    source.onNext(5);

    source.onComplete();

    firstObserver.assertValue(
        ImmutableMap.<Integer, Integer>builder()
            .putMapping(0, 0)
            .putMapping(1, 1)
            .putMapping(2, 2)
            .putMapping(3, 3)
            .putMapping(4, 4)
            .putMapping(5, 5)
            .build())
        .assertComplete();
    secondObserver.assertValue(
        ImmutableMap.<Integer, Integer>builder()
            .putMapping(3, 3)
            .putMapping(4, 4)
            .putMapping(5, 5)
            .build())
        .assertComplete();
    underTest.test().assertValue(ImmutableMap.empty()).assertComplete();
  }

  @Test
  public void toImmutableSet_withItems_emitsItemsAsSet() {
    Observable.just(1, 2, 3, 4, 5)
        .to(Observables.toImmutableSet())
        .test()
        .assertValue(ImmutableSet.of(1, 2, 3, 4, 5))
        .assertComplete();
  }

  @Test
  public void toImmutableSet_whenEmpty_emitsEmptySet() {
    Observable.empty()
        .to(Observables.toImmutableSet())
        .test()
        .assertValue(ImmutableSet.empty())
        .assertComplete();
  }

  @Test
  public void toImmutableSet_whenNeverCompletes_emitsNothing() {
    Observable.never()
        .to(Observables.toImmutableSet())
        .test()
        .assertNoValues()
        .assertNotComplete();
  }

  @Test
  public void toImmutableSet_whenHot_withLateSubscribers_doesNotShareState() {
    Subject<Integer> source = PublishSubject.create();

    Single<ImmutableSet<Integer>> underTest = source.to(Observables.toImmutableSet());

    TestObserver<ImmutableSet<Integer>> firstObserver = underTest.test();

    source.onNext(0);
    source.onNext(1);
    source.onNext(2);

    TestObserver<ImmutableSet<Integer>> secondObserver = underTest.test();

    source.onNext(3);
    source.onNext(4);
    source.onNext(5);

    source.onComplete();

    firstObserver.assertValue(ImmutableSet.of(0, 1, 2, 3, 4, 5)).assertComplete();
    secondObserver.assertValue(ImmutableSet.of(3, 4, 5)).assertComplete();
    underTest.test().assertValue(ImmutableSet.empty()).assertComplete();
  }
}
