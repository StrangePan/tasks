package tasks.cli.feature.remove;

import static java.util.Objects.requireNonNull;
import static omnia.data.cache.Memoized.memoize;
import static tasks.util.rx.Observables.toImmutableSet;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Scanner;
import omnia.cli.out.Output;
import omnia.data.cache.Memoized;
import omnia.data.structure.Collection;
import tasks.cli.handler.ArgumentHandler;
import tasks.cli.handler.HandlerException;
import tasks.cli.handler.HandlerUtil;
import tasks.model.Task;
import tasks.model.ObservableTaskStore;

/** Business logic for the Remove command. */
public final class RemoveHandler implements ArgumentHandler<RemoveArguments> {
  private final Memoized<? extends ObservableTaskStore> taskStore;

  public RemoveHandler(Memoized<? extends ObservableTaskStore> taskStore) {
    this.taskStore = requireNonNull(taskStore);
  }

  @Override
  public Single<Output> handle(RemoveArguments arguments) {
    Collection<Task> tasksToDelete = arguments.tasks();

    // Validate arguments
    if (!tasksToDelete.isPopulated()) {
      throw new HandlerException("no tasks specified");
    }

    return Observable.fromIterable(tasksToDelete)
        .compose(
            observable ->
                arguments.force()
                    ? observable
                    : observable.concatMapMaybe(
                        task ->
                            getYesNoConfirmationFor(task)
                                .filter(confirm -> confirm).map(u -> task)))
        .flatMapMaybe(task -> taskStore.value().deleteTask(task))
        .to(toImmutableSet())
        .map(deletedTasks -> HandlerUtil.stringifyIfPopulated("tasks deleted:", deletedTasks));
  }

  private static Single<Boolean> getYesNoConfirmationFor(Task task) {
    return Single.just(task)
        .map(Task::render)
        .map(Output::render)
        .doOnSuccess(System.out::println)
        .ignoreElement()
        .andThen(getYesNoConfirmation());
  }

  private static Single<Boolean> getYesNoConfirmation() {
    Memoized<RuntimeException> unparsedInput = memoize(RuntimeException::new);
    return Single.just(
        Output.builder()
            .color(Output.Color16.YELLOW)
            .append("Delete this task? [Y/n]: ")
            .defaultColor()
            .build())
        .map(Output::render)
        .doOnSuccess(System.out::print)
        .ignoreElement()
        .andThen(readUserInput())
        .map(
            input -> {
              if (input.matches("\\s*[Yy]([Ee][Ss])?\\s*")) {
                return true;
              } else if (input.matches("\\s*[Nn]([Oo])?\\s*")) {
                return false;
              } else {
                throw unparsedInput.value();
              }
            })
        .retry(2, throwable -> throwable == unparsedInput.value())
        .onErrorReturn(
            throwable -> {
              if (throwable == unparsedInput.value()) {
                return false;
              }
              throw new RuntimeException(throwable);
            });
  }

  private static Single<String> readUserInput() {
    return Single.fromCallable(() ->new Scanner(System.in).nextLine());
  }
}
