package tasks.cli.feature.reword

import io.reactivex.rxjava3.core.Single
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.data.cache.Memoized
import tasks.cli.command.common.CommonArguments
import tasks.cli.handler.ArgumentHandler
import tasks.cli.handler.HandlerException
import tasks.model.ObservableTaskStore

class RewordHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<RewordArguments> {

  override fun handle(arguments: CommonArguments<out RewordArguments>): Single<Output> {
    val description = arguments.specificArguments().description().trim { it <= ' ' }
    if (description.isEmpty()) {
      throw HandlerException("description cannot be empty or whitespace only")
    }
    return Single.fromCallable { taskStore.value() }
        .flatMap { store ->
          store
              .mutateTask(arguments.specificArguments().targetTask()) {
                it.setLabel(arguments.specificArguments().description())
              }
              .map { it.third() }
        }
        .map { it.render() }
        .map { taskOutput ->
          builder()
              .append("Updated description: ")
              .append(taskOutput)
              .appendLine()
              .build()
        }
  }

}