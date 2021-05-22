package me.strangepan.tasks.cli.feature.add

import io.reactivex.rxjava3.core.Single
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.builder
import omnia.data.cache.Memoized
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.ArgumentHandler
import me.strangepan.tasks.cli.handler.HandlerException
import me.strangepan.tasks.cli.model.render
import me.strangepan.tasks.engine.model.ObservableTaskStore

/** Business logic for the Add command.  */
class AddHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<AddArguments> {

  override fun handle(arguments: CommonArguments<out AddArguments>): Single<Output> {
    // Validate arguments
    val description = arguments.specificArguments().description().trim { it <= ' ' }
    if (description.isEmpty()) {
      throw HandlerException("description cannot be empty or whitespace only")
    }

    // Construct the new task, commit to disk, print output
    return taskStore.value()
        .createTask(description) {
            it.setBlockingTasks(arguments.specificArguments().blockingTasks())
                .setBlockedTasks(arguments.specificArguments().blockedTasks())
    }
      .map { it.third() }
      .map { it.render() }
      .map { builder().append("task created: ").append(it).build() }
      .map { builder().appendLine(it).build() }
  }

}