package me.strangepan.tasks.cli.handler.testing

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.SingleSubject
import java.util.Objects
import me.strangepan.tasks.cli.input.Reader
import omnia.data.structure.mutable.ArrayQueue
import omnia.data.structure.mutable.Queue

class TestReader private constructor() : Reader {
  private val lock = Any()
  private val queuedInput: Queue<String> = ArrayQueue.create()
  private val queuedOutput: Queue<SingleSubject<String>> = ArrayQueue.create()
  override fun readNextLine(): Single<String> {
    synchronized(lock) {
      return queuedInput.dequeue()
          .map { Single.just(it) }
          .orElseGet(::createPendingOutputSubject)
    }
  }

  private fun createPendingOutputSubject(): Single<String> {
    val subject = SingleSubject.create<String>()
    queuedOutput.enqueue(subject)
    return subject.hide()
  }

  /**
   * Enqueues the given line or forwards these line to any pending subscribers.
   *
   *
   * If there are any pending reads from [.readNextLine], immediately forwards the line
   * to that Single. Otherwise, enqueues the line so that the next [.readNextLine] call
   * will immediately return the line.
   *
   * @param line The string to enqueue to the output buffer or to immediately send to pending
   * readers.
   * @return this object to enable method chaining
   */
  fun putLine(line: String): TestReader {
    Objects.requireNonNull(line)
    synchronized(lock) {
      queuedOutput.dequeue()
          .ifPresentOrElse({ subject: SingleSubject<String> -> subject.onSuccess(line) }) { queuedInput.enqueue(line) }
    }
    return this
  }

  companion object {
    fun create(): TestReader {
      return TestReader()
    }
  }
}