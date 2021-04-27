package me.strangepan.tasks.cli.feature.list

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import omnia.cli.out.Output
import omnia.data.cache.Memoized
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.tuple.Triple
import omnia.data.structure.tuple.Tuple
import me.strangepan.tasks.cli.command.common.CommonArguments
import me.strangepan.tasks.cli.handler.ArgumentHandler
import me.strangepan.tasks.cli.handler.HandlerUtil
import me.strangepan.tasks.engine.model.ObservableTaskStore
import me.strangepan.tasks.engine.model.Task
import me.strangepan.tasks.engine.util.rx.Observables

/** Business logic for the List command.  */
class ListHandler(private val taskStore: Memoized<out ObservableTaskStore>) : ArgumentHandler<ListArguments> {
  override fun handle(arguments: CommonArguments<out ListArguments>): Single<Output> {
    return Single.fromCallable { taskStore.value() }
        .flatMapObservable(ObservableTaskStore::observe)
        .firstOrError()
        .flatMapObservable { store ->
          Observable.just<Triple<Boolean, String, out Single<out ImmutableSet<out Task>>>>(
              Tuple.of(
                  arguments.specificArguments().isUnblockedSet,
                  "unblocked tasks:",
                  Single.fromCallable(store::allOpenTasksWithoutOpenBlockers)),
              Tuple.of(
                  arguments.specificArguments().isBlockedSet,
                  "blocked tasks:",
                  Single.fromCallable(store::allOpenTasksWithOpenBlockers)),
              Tuple.of(
                  arguments.specificArguments().isCompletedSet,
                  "completed tasks:",
                  Single.fromCallable(store::allCompletedTasks)))
        }
        .filter { it.first() }
        .map { it.dropFirst() }
        .map { couple -> if (arguments.specificArguments().isStartedSet) couple.mapSecond(::filterOutUnstartedTasks) else couple }
        .concatMapEager { couple -> couple.second().map { tasks -> Tuple.of(couple.first(), tasks) }.toObservable() }
        .map { couple -> HandlerUtil.stringifyIfPopulated(couple.first(), couple.second()) }
        .collect(Output::builder, Output.Builder::appendLine)
        .map(Output.Builder::build)
  }

  companion object {
    private fun <T : Task> filterOutUnstartedTasks(
        tasks: Single<out ImmutableSet<out T>>): Single<ImmutableSet<T>> {
      return tasks.flatMapObservable { source -> Observable.fromIterable(source) }
          .filter { it.status().isStarted }
          .map { it }
          .to(Observables.toImmutableSet())
    }
  }

}