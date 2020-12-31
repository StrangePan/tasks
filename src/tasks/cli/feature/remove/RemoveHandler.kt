package tasks.cli.feature.remove;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static tasks.util.rx.Observables.toImmutableSet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import tasks.cli.command.common.CommonArguments;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.HandlerUtil;
import tasks.cli.input.Reader;
import tasks.cli.output.Printer;
import tasks.model.Task;
import tasks.model.ObservableTaskStore;

/** Business logic for the Remove command. */
public final class RemoveHandler implements ArgumentHandler<RemoveArguments> {

  private final Memoized<? extends ObservableTaskStore> taskStore;
  private final Printer.Factory printerFactory;
  private final Memoized<? extends Reader> reader;

  public RemoveHandler(
      Memoized<? extends ObservableTaskStore> taskStore,
      Printer.Factory printerFactory,
      Memoized<? extends Reader> reader) {
    this.taskStore = requireNonNull(taskStore);
    this.printerFactory = requireNonNull(printerFactory);
    this.reader = requireNonNull(reader);
  }

  @Override
  public Single<Output> handle(CommonArguments<? extends RemoveArguments> arguments) {
    Collection<Task> tasksToDelete = arguments.specificArguments().tasks();

    // Validate arguments
    if (!tasksToDelete.isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    Memoized<Printer> printer = memoize(() -> printerFactory.create(arguments));

    return Observable.fromIterable(tasksToDelete)
        .compose(
            observable ->
                arguments.specificArguments().force()
                    ? observable
                    : observable.concatMapMaybe(
                        task ->
                            getYesNoConfirmationFor(task, printer, reader)
                                .filter(confirm -> confirm).map(u -> task)))
        .flatMapMaybe(task -> taskStore.value().deleteTask(task))
        .to(toImmutableSet())
        .map(deletedTasks -> HandlerUtil.stringifyIfPopulated("tasks deleted:", deletedTasks));
  }

  private static Single<Boolean> getYesNoConfirmationFor(
      Task task, Memoized<? extends Printer> printer, Memoized<? extends Reader> reader) {
    return Single.just(task)
        .map(Task::render)
        .doOnSuccess(output -> printer.value().printLine(output))
        .ignoreElement()
        .andThen(getYesNoConfirmation(printer, reader));
  }

  private static Single<Boolean> getYesNoConfirmation(
      Memoized<? extends Printer> printer, Memoized<? extends Reader> reader) {
    Memoized<RuntimeException> unparsedInput = memoize(RuntimeException::new);
    return Single.just(
        Output.builder()
            .color(Output.Color16.YELLOW)
            .append("Delete this task? [Y/n]: ")
            .defaultColor()
            .build())
        .doOnSuccess(output -> printer.value().print(output))
        .ignoreElement()
        .andThen(Single.defer(() -> reader.value().readNextLine()))
        .map(
            input -> {
              if (input.matches("\\s*[Yy]([Ee][Ss])?\\s*")) {
                return true;
              } else if (input.matches("\\s*[Nn]([Oo])?\\s*")) {
                return false;
              } else {
                printer.value().print(
                    Output.builder()
                        .color(Output.Color16.YELLOW)
                        .append("Unrecognized answer. ")
                        .defaultColor()
                        .build());
                throw unparsedInput.value();
              }
            })
        .retry(2, throwable -> throwable == unparsedInput.value())
        .onErrorReturn(
            throwable -> {
              if (throwable == unparsedInput.value()) {
                printer.value().print(
                    Output.builder()
                        .color(Output.Color16.YELLOW)
                        .appendLine("Assuming \"No\".")
                        .defaultColor()
                        .build());
                return false;
              }
              throw new RuntimeException(throwable);
            })
        .cache();
  }
}
