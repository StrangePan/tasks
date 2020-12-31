package tasks.util.rx

import io.reactivex.rxjava3.core.Maybe
import java.util.Optional

object Maybes {
  @JvmStatic
  fun <T> fromOptional(optional: Optional<T>): Maybe<T> {
    return optional.map { item: T -> Maybe.just(item) }.orElse(Maybe.empty())
  }
}