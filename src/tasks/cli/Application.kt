package tasks.cli

import io.reactivex.rxjava3.core.Single
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList
import tasks.cli.feature.Feature
import tasks.cli.feature.Features
import tasks.model.impl.ObservableTaskStoreImpl

internal class Application(private val rawArgs: Array<String>) {
  private val taskStore: Memoized<ObservableTaskStoreImpl> =
    memoize { ObservableTaskStoreImpl.createFromFile(".tasks") }
  private val features = memoize { Features(taskStore) }

  fun run() {
    Single.just(rawArgs)
        .map { obj -> ImmutableList.copyOf(obj) }
        .flatMapCompletable { args -> getHelpOrFallbackFeature(args).handle(args, taskStore) }
        .andThen(taskStore.value().shutdown())
        .blockingAwait()
  }

  private fun getHelpOrFallbackFeature(args: List<out String>): Feature<*> {
    return args.stream()
        .findFirst()
        .map { arg -> features.value().getMatchingOrFallbackFeature(arg) }
        .orElseGet { features.value().fallbackFeature }
  }

}