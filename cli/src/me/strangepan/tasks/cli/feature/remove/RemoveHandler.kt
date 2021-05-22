package me.strangepan.tasks.cli.feature.remove

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.Collection
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.ArgumentHandler
import me.strangepan.tasks.cli.handler.HandlerException
import me.strangepan.tasks.cli.handler.HandlerUtil
import me.strangepan.tasks.cli.input.Reader
import me.strangepan.tasks.cli.model.render
import me.strangepan.tasks.cli.output.Printer
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.util.rx.Observables

/** Business logic for the Remove command.  */
class RemoveHandler(
    private val taskStore: Memoized<out ObservableTaskStore>,
    private val printerFactory: Printer.Factory,
    private val reader: Memoized<out Reader>) : ArgumentHandler<RemoveArguments> {

  override fun handle(arguments: CommonArguments<out RemoveArguments>): Single<Output> {
    val tasksToDelete: Collection<Task> = arguments.specificArguments().tasks()

    // Validate arguments
    if (!tasksToDelete.isPopulated) {
      throw HandlerException("no tasks specified")
    }
    val printer: Memoized<Printer> = memoize { printerFactory.create(arguments) }
    return Observable.fromIterable(tasksToDelete)
        .compose { observable ->
          if (arguments.specificArguments().force()) observable else observable.concatMapMaybe { task ->
            getYesNoConfirmationFor(task, printer, reader)
                .filter { it }
                .map { task }
          }
        }
        .flatMapMaybe { taskStore.value().deleteTask(it) }
        .to(Observables.toImmutableSet())
        .map { HandlerUtil.stringifyIfPopulated("tasks deleted:", it) }
  }

  companion object {
    private fun getYesNoConfirmationFor(
        task: Task, printer: Memoized<out Printer>, reader: Memoized<out Reader>): Single<Boolean> {
      return Single.just(task)
          .map(Task::render)
          .doOnSuccess { printer.value().printLine(it) }
          .ignoreElement()
          .andThen(getYesNoConfirmation(printer, reader))
    }

    private fun getYesNoConfirmation(
        printer: Memoized<out Printer>, reader: Memoized<out Reader>): Single<Boolean> {
      val unparsedInput = memoize(::RuntimeException)
      return Single.just(
          builder()
              .color(Output.Color16.YELLOW)
              .append("Delete this task [Y/n]: ")
              .defaultColor()
              .build())
          .doOnSuccess { printer.value().print(it) }
          .ignoreElement()
          .andThen(Single.defer { reader.value().readNextLine() })
          .map { input ->
            when {
              input.matches("""\s*[Yy]([Ee][Ss])?\s*""".toRegex()) -> {
                true
              }
              input.matches("""\s*[Nn]([Oo])?\s*""".toRegex()) -> {
                false
              }
              else -> {
                printer.value().print(
                    builder()
                        .color(Output.Color16.YELLOW)
                        .append("Unrecognized answer. ")
                        .defaultColor()
                        .build())
                throw unparsedInput.value()
              }
            }
          }
          .retry(2) { throwable: Throwable -> throwable === unparsedInput.value() }
          .onErrorReturn { throwable ->
            if (throwable === unparsedInput.value()) {
              printer.value().print(
                  builder()
                      .color(Output.Color16.YELLOW)
                      .appendLine("Assuming \"No\".")
                      .defaultColor()
                      .build())
              return@onErrorReturn false
            }
            throw RuntimeException(throwable)
          }
          .cache()
    }
  }

}