package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import java.util.function.Supplier;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import tasks.cli.arg.AddArguments;
import tasks.cli.arg.AmendArguments;
import tasks.cli.arg.CompleteArguments;
import tasks.cli.arg.HelpArguments;
import tasks.cli.arg.InfoArguments;
import tasks.cli.arg.ListArguments;
import tasks.cli.arg.RemoveArguments;
import tasks.cli.arg.ReopenArguments;

public final class ArgumentHandlers implements ArgumentHandler<Object> {

  private final ImmutableMap<Class<?>, ArgumentHandler<Object>> registeredHandlers;

  public static ArgumentHandlers create() {
    return new ArgumentHandlers();
  }

  private ArgumentHandlers() {
    this.registeredHandlers = buildHandlerMap();
  }

  private static ImmutableMap<Class<?>, ArgumentHandler<Object>> buildHandlerMap() {
    return new RegistryBuilder()
        .register(AddArguments.class, AddHandler::new)
        .register(AmendArguments.class, AmendHandler::new)
        .register(CompleteArguments.class, CompleteHandler::new)
        .register(HelpArguments.class, HelpHandler::new)
        .register(InfoArguments.class, InfoHandler::new)
        .register(ListArguments.class, ListHandler::new)
        .register(RemoveArguments.class, RemoveHandler::new)
        .register(ReopenArguments.class, ReopenHandler::new)
        .build();
  }

  @Override
  public Completable handle(Object arguments) {
    Class<?> argumentsClass = requireNonNull(arguments).getClass();
    return registeredHandlers.valueOf(argumentsClass)
        .orElseThrow(
            () -> new IllegalArgumentException("Unsupported argument type " + argumentsClass))
        .handle(arguments);
  }

  private static final class RegistryBuilder {
    private final MutableMap<Class<?>, ArgumentHandler<Object>> registeredHandlers =
        HashMap.create();

    <T> RegistryBuilder register(Class<T> argumentsClass, Supplier<? extends ArgumentHandler<? super T>> handlerSupplier) {
      requireNonNull(argumentsClass);
      requireNonNull(handlerSupplier);
      requireUnique(argumentsClass);
      registeredHandlers.putMapping(
          argumentsClass, ValidatingHandler.validate(argumentsClass, handlerSupplier));
      return this;
    }

    private void requireUnique(Class<?> argumentsClass) {
      if (registeredHandlers.keys().contains(argumentsClass)) {
        throw new IllegalStateException("Duplication registration for " + argumentsClass);
      }
    }

    ImmutableMap<Class<?>, ArgumentHandler<Object>> build() {
      return ImmutableMap.copyOf(registeredHandlers);
    }
  }
}
