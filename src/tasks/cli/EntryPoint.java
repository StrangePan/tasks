package tasks.cli;

import static java.util.Objects.requireNonNull;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import java.util.function.Supplier;
import omnia.data.structure.immutable.ImmutableMap;
import tasks.cli.arg.AddArguments;
import tasks.cli.arg.AmendArguments;
import tasks.cli.arg.CliArguments;
import tasks.cli.arg.CompleteArguments;
import tasks.cli.arg.HelpArguments;
import tasks.cli.arg.InfoArguments;
import tasks.cli.arg.ListArguments;
import tasks.cli.arg.RemoveArguments;
import tasks.cli.arg.ReopenArguments;
import tasks.cli.handlers.AddHandler;
import tasks.cli.handlers.AmendHandler;
import tasks.cli.handlers.ArgumentHandler;
import tasks.cli.handlers.CompleteHandler;
import tasks.cli.handlers.HelpHandler;
import tasks.cli.handlers.InfoHandler;
import tasks.cli.handlers.ListHandler;
import tasks.cli.handlers.RemoveHandler;
import tasks.cli.handlers.ReopenHandler;

/** CLI entry point into the Tasks application. */
public final class EntryPoint {

  public static void main(String[] args) {
    Maybe.just(args)
        .compose(EntryPoint::parseCliArguments)
        .compose(EntryPoint::handleCliArguments)
        .flatMapCompletable(c -> c)
        .blockingAwait();
  }

  private static Maybe<CliArguments> parseCliArguments(Maybe<String[]> args) {
    return args
        .map(CliArguments::parse)
        .doOnError(e -> System.out.println(e.getMessage()))
        .onErrorComplete();
  }

  private static Maybe<Completable> handleCliArguments(Maybe<CliArguments> args) {
    return args.map(arguments ->
        handlers()
            .valueOf(arguments.getArguments().getClass())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Handler not defined for mode " + arguments.getModeArgument()))
            .handle(arguments.getArguments()));
  }

  private static ImmutableMap<Class<?>, ArgumentHandler<Object>> handlers() {
    return ImmutableMap.<Class<?>, ArgumentHandler<Object>>builder()
        .put(AddArguments.class, validate(AddArguments.class, AddHandler::new))
        .put(AmendArguments.class, validate(AmendArguments.class, AmendHandler::new))
        .put(CompleteArguments.class, validate(CompleteArguments.class, CompleteHandler::new))
        .put(HelpArguments.class, validate(HelpArguments.class, HelpHandler::new))
        .put(InfoArguments.class, validate(InfoArguments.class, InfoHandler::new))
        .put(ListArguments.class, validate(ListArguments.class, ListHandler::new))
        .put(RemoveArguments.class, validate(RemoveArguments.class, RemoveHandler::new))
        .put(ReopenArguments.class, validate(ReopenArguments.class, ReopenHandler::new))
        .build();
  }

  private static <T> ValidatingHandler<T> validate(
      Class<? extends T> clazz, Supplier<ArgumentHandler<? super T>> supplier) {
    return new ValidatingHandler<>(clazz, supplier);
  }

  private static final class ValidatingHandler<T> implements ArgumentHandler<Object> {
    private final Class<? extends T> clazz;
    private final Supplier<ArgumentHandler<? super T>> supplier;

    private ValidatingHandler(
        Class<? extends T> clazz, Supplier<ArgumentHandler<? super T>> supplier) {
      this.clazz = requireNonNull(clazz);
      this.supplier = requireNonNull(supplier);
    }

    @Override
    public Completable handle(Object arguments) {
      if (clazz.isAssignableFrom(arguments.getClass())) {
        return supplier.get().handle(clazz.cast(arguments));
      } else {
        throw new IllegalArgumentException("invalid argument type passed to handler");
      }
    }
  }
}
