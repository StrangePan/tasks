package tasks.cli.input;

import static java.util.Objects.requireNonNull;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.SingleSubject;
import omnia.data.structure.mutable.ArrayQueue;
import omnia.data.structure.mutable.Queue;

public final class TestReader implements Reader {

  private final Object lock = new Object();
  private final Queue<String> queuedInput = ArrayQueue.create();
  private final Queue<SingleSubject<String>> queuedOutput = ArrayQueue.create();

  public static TestReader create() {
    return new TestReader();
  }

  private TestReader() {}

  @Override
  public Single<String> readNextLine() {
    synchronized (lock) {
      return queuedInput.dequeue().map(Single::just).orElseGet(this::createPendingOutputSubject);
    }
  }

  private Single<String> createPendingOutputSubject() {
    SingleSubject<String> subject = SingleSubject.create();
    queuedOutput.enqueue(subject);
    return subject.hide();
  }

  /**
   * Enqueues the given line or forwards these line to any pending subscribers.
   *
   * <p>If there are any pending reads from {@link #readNextLine()}, immediately forwards the line
   * to that Single. Otherwise, enqueues the line so that the next {@link #readNextLine()} call
   * will immediately return the line.
   *
   * @param line The string to enqueue to the output buffer or to immediately send to pending
   *     readers.
   * @return this object to enable method chaining
   */
  public TestReader putLine(String line) {
    requireNonNull(line);
    synchronized (lock) {
      queuedOutput.dequeue()
          .ifPresentOrElse(subject -> subject.onSuccess(line), () -> queuedInput.enqueue(line));
    }
    return this;
  }
}
