package tasks.cli.input;

import io.reactivex.Single;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Simple abstraction for reading input from the command line. Useful for controlling input for
 * testing.
 */
public interface Reader {

  /**
   * Reads the next line of user input from the command line and returns a {@link Single} that will
   * complete with the next line when a user has input it. The Single does not subscribe on any
   * particular scheduler and may block the thread that subscribes to it. All subscribers to the
   * single share the same subscription, and the Single caches the result.
   */
  Single<String> readNextLine();

  static Reader forInputStream(InputStream inputStream) {
    return () -> Single.fromCallable(() -> new Scanner(inputStream).nextLine()).cache();
  }
}
