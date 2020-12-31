package tasks.cli.input

import io.reactivex.rxjava3.core.Single
import java.io.InputStream
import java.util.Scanner

/**
 * Simple abstraction for reading input from the command line. Useful for controlling input for
 * testing.
 */
interface Reader {
  /**
   * Reads the next line of user input from the command line and returns a [Single] that will
   * complete with the next line when a user has input it. The Single does not subscribe on any
   * particular scheduler and may block the thread that subscribes to it. All subscribers to the
   * single share the same subscription, and the Single caches the result.
   */
  fun readNextLine(): Single<String>

  companion object {
    fun forInputStream(inputStream: InputStream): Reader {
      return object : Reader {
        override fun readNextLine(): Single<String> {
          return Single.fromCallable { Scanner(inputStream).nextLine() }.cache()
        }
      }
    }
  }
}