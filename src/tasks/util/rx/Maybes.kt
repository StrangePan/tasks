package tasks.util.rx;

import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;

public final class Maybes {

  private Maybes() {}

  public static <T> Maybe<T> fromOptional(Optional<T> optional) {
    return optional.map(Maybe::just).orElse(Maybe.empty());
  }
}
