package tasks.cli.handlers;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import java.util.function.Supplier;
import omnia.data.cache.Memoized;
import omnia.data.structure.Set;
import omnia.data.structure.immutable.ImmutableMap;
import omnia.data.structure.mutable.HashMap;
import omnia.data.structure.mutable.MutableMap;
import tasks.cli.command.add.AddArguments;
import tasks.cli.command.amend.AmendArguments;
import tasks.cli.command.complete.CompleteArguments;
import tasks.cli.command.help.HelpArguments;
import tasks.cli.command.info.InfoArguments;
import tasks.cli.command.list.ListArguments;
import tasks.cli.arg.RemoveArguments;
import tasks.cli.arg.ReopenArguments;
import tasks.cli.arg.CommandDocumentation;
import tasks.cli.command.add.AddHandler;
import tasks.cli.command.amend.AmendHandler;
import tasks.cli.command.complete.CompleteHandler;
import tasks.cli.command.help.HelpHandler;
import tasks.cli.command.info.InfoHandler;
import tasks.cli.command.list.ListHandler;
import tasks.model.TaskStore;

public final class ArgumentHandlers implements ArgumentHandler<Object> {
  private final ImmutableMap<Class<?>, ArgumentHandler<Object>> registeredHandlers;

  public static ArgumentHandlers create(
      Memoized<TaskStore> taskStore, Memoized<Set<CommandDocumentation>> documentation) {
    return new ArgumentHandlers(requireNonNull(taskStore), requireNonNull(documentation));
  }

  private ArgumentHandlers(
      Memoized<TaskStore> taskStore, Memoized<Set<CommandDocumentation>> documentation) {
    this.registeredHandlers = buildHandlerMap(taskStore, documentation);
  }

  private static ImmutableMap<Class<?>, ArgumentHandler<Object>> buildHandlerMap(
      Memoized<TaskStore> taskStore,
      Memoized<Set<CommandDocumentation>> documentation) {
    return new RegistryBuilder()
        .register(AddArguments.class, () -> new AddHandler(taskStore))
        .register(AmendArguments.class, () -> new AmendHandler(taskStore))
        .register(CompleteArguments.class, () -> new CompleteHandler(taskStore))
        .register(HelpArguments.class, () -> new HelpHandler(documentation))
        .register(InfoArguments.class, InfoHandler::new)
        .register(ListArguments.class, () -> new ListHandler(taskStore))
        .register(RemoveArguments.class, () -> new RemoveHandler(taskStore))
        .register(ReopenArguments.class, () -> new ReopenHandler(taskStore))
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
