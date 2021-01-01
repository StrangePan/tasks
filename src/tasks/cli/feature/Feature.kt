package tasks.cli.feature

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleConverter
import java.io.PrintStream
import java.util.Objects
import java.util.function.Supplier
import omnia.algorithm.ListAlgorithms.toArray
import omnia.cli.out.Output
import omnia.data.cache.Memoized
import omnia.data.stream.Collectors.toImmutableList
import omnia.data.structure.List
import omnia.data.structure.Set
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Couple
import omnia.data.structure.tuple.Tuple
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import tasks.cli.command.Command
import tasks.cli.command.Option
import tasks.cli.command.common.CommonArguments
import tasks.cli.command.common.CommonOptions
import tasks.cli.command.common.CommonParser
import tasks.cli.handler.ArgumentHandler
import tasks.cli.output.Printer
import tasks.cli.parser.CommandParser
import tasks.cli.parser.ParserException
import tasks.model.ObservableTaskStore
import tasks.model.impl.CyclicalDependencyException

class Feature<T : Any> internal constructor(
    val command: Command,
    private val parser: Supplier<out CommandParser<out T>>,
    private val handler: Supplier<out ArgumentHandler<in T>>) {

  private fun obtainParser(): CommandParser<out T> {
    return parser.get()
  }

  private fun obtainCommonParser(): CommonParser {
    return CommonParser()
  }

  private fun obtainHandler(): ArgumentHandler<in T> {
    return Objects.requireNonNull(handler.get())
  }

  fun handle(
      args: List<out String>,
      taskStore: Memoized<out ObservableTaskStore>): Completable {
    return Single.just(args)
        .to(SingleConverter(::toArguments))
        .flatMap { arguments -> toOutput(arguments).map { Tuple.of(arguments, it) } }
        .flatMap { taskStore.value().shutdown().andThen(Single.just(it)) }
        .flatMapCompletable(::printToMore)
        .to(::maybeConsumeError)
  }

  private fun printToMore(argumentsAndOutput: Couple<out CommonArguments<*>, Output>): Completable {
    val arguments = argumentsAndOutput.first()
    val output = argumentsAndOutput.second()

    return Completable.create { emitter ->
      val process = ProcessBuilder("more")
          .directory(null)
          .inheritIO()
          .redirectInput(ProcessBuilder.Redirect.PIPE)
          .start()

      PrintStream(process.outputStream).use {
        if (!emitter.isDisposed) {
          Printer.Factory { it }.create(arguments).print(output)
        }
      }

      emitter.setCancellable(process::destroy)
      process.waitFor()
      emitter.onComplete()
    }
  }

  private fun toArguments(args: Single<out List<out String>>): Single<CommonArguments<out T>> {
    return args
        .map(::dropFirstValue)
        .map(::toArgsAndCommand)
        .flatMap(::toCommandLine)
        .map { obtainCommonParser().parse(it, obtainParser().parse(it)) }
  }

  private fun toArgsAndCommand(args: List<out String>): Couple<List<out String>, Command> {
    return Tuple.of(args, command)
  }

  private fun toCommandLine(
      couple: Couple<out List<out String>, Command>): Single<CommandLine> {
    return Observable.concat(
        Single.just(couple)
            .map { it.second().options() }
            .flatMapObservable { Observable.fromIterable(it) },
        Single.fromCallable { CommonOptions.OPTIONS.value() }
            .flatMapObservable { Observable.fromIterable(it) })
        .to(::toOptions)
        .map { tryParse(couple.first(), it) }
  }

  private fun toOutput(arguments: CommonArguments<out T>): Single<Output> {
    return obtainHandler().handle(arguments)
  }

  companion object {
    private fun <E> dropFirstValue(list: List<out E>): ImmutableList<E> {
      return list.stream().skip(1).collect(toImmutableList())
    }

    private fun toOptions(optionStream: Observable<out Option>): Single<Options> {
      return optionStream.map(Option::toCliOption)
          .collect(::Options) { options, option -> options.addOption(option) }
    }

    private fun tryParse(args: List<out String>, options: Options): CommandLine {
      return try {
        DefaultParser()
            .parse(
                options, toArray(args, arrayOfNulls(0)),  /* stopAtNonOption= */false)
      } catch (e: ParseException) {
        throw ParserException("Unable to parse arguments: ${e.message}", e)
      }
    }

    private fun maybeConsumeError(completable: Completable): Completable {
      return completable
          .doOnError(::maybeConsumeError)
          .onErrorComplete(::maybeSuppressError)
    }

    private fun maybeConsumeError(throwable: Throwable) {
      if (shouldSuppressError(throwable)) {
        println(throwable.message)
      }
    }

    private fun maybeSuppressError(throwable: Throwable): Boolean {
      return shouldSuppressError(throwable)
    }

    private fun shouldSuppressError(throwable: Throwable): Boolean {
      for (suppressedClass in SUPPRESSED_HANDLER_THROWABLE_CLASSES) {
        if (suppressedClass.isInstance(throwable)) {
          return true
        }
      }
      return false
    }

    // TODO: don't hard-code throwable classes.
    // We need a better way of converting an exception into a proper, human-readable output.
    private val SUPPRESSED_HANDLER_THROWABLE_CLASSES: Set<Class<out Throwable>> =
        ImmutableSet.of(ParserException::class.java, CyclicalDependencyException::class.java)
  }

}