package me.strangepan.tasks.cli.feature.reword

import io.reactivex.rxjava3.core.Single
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.data.cache.Memoized
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.ArgumentHandler
import me.strangepan.tasks.cli.handler.HandlerException
import me.strangepan.tasks.cli.model.render
import me.strangepan.tasks.engine.model.ObservableTaskStore

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