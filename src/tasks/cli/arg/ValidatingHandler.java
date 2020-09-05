package tasks.cli.arg;

import static java.util.Objects.requireNonNull;

import io.reactivex.Single;
import java.util.function.Supplier;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import tasks.cli.handler.ArgumentHandler;

final class ValidatingHandler<T> implements ArgumentHandler<Object> {
  private final Class<? extends T> argumentClass;
  private final Memoized<ArgumentHandler<? super T>> delegateHandler;

  static <T> ValidatingHandler<T> validate(
      Class<? extends T> clazz, Supplier<? extends ArgumentHandler<? super T>> supplier) {
    return new ValidatingHandler<>(clazz, supplier);
  }

  private ValidatingHandler(
      Class<? extends T> argumentClass,
      Supplier<? extends ArgumentHandler<? super T>> handlerSupplier) {
    this.argumentClass = requireNonNull(argumentClass);
    this.delegateHandler = Memoized.memoize(requireNonNull(handlerSupplier));
  }

  @Override
  public Single<Output> handle(Object arguments) {
    if (argumentClass.isAssignableFrom(arguments.getClass())) {
      return delegateHandler.value().handle(argumentClass.cast(arguments));
    } else {
      throw new IllegalArgumentException("invalid argument type passed to handler");
    }
  }
}
