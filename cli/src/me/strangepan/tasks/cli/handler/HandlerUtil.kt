package me.strangepan.tasks.cli.handler

import io.reactivex.rxjava3.core.Observable
import java.util.Comparator
import java.util.Locale
import java.util.Optional
import java.util.stream.Collectors.joining
import omnia.algorithm.SetAlgorithms.intersectionOf
import omnia.cli.out.Output
import omnia.cli.out.Output.Companion.empty
import omnia.contract.Countable
import omnia.data.stream.Collectors.toImmutableSet
import omnia.data.structure.Collection
import omnia.data.structure.immutable.ImmutableSet
import me.strangepan.tasks.engine.model.Task

object HandlerUtil {

  fun stringifyIfPopulated(prefix: String, tasks: Collection<out Task>): Output {
    return if (tasks.isPopulated) Output.builder()
        .color(Output.Color16.LIGHT_MAGENTA)
        .appendLine(prefix)
        .defaultColor()
        .appendLine(stringify(tasks), 2)
        .build() else empty()
  }

  private fun stringify(tasks: Iterable<Task>): Output {
    return Observable.fromIterable(tasks)
        .sorted(
            Comparator.comparingInt<Task> { task -> if (task.status().isStarted) 0 else 1 }
                .thenComparing { task -> task.label().toLowerCase(Locale.ROOT) }
                .thenComparing { task -> task.id().toString() })
        .map(Task::render)
        .collect(Output.Companion::builder, Output.Builder::appendLine)
        .map(Output.Builder::build)
        .blockingGet()
  }

  fun verifyTasksAreMutuallyExclusive(
      failurePrefix: String, first: Collection<out Task>, second: Collection<out Task>) {
    Optional.of(
        intersectionOf(ImmutableSet.copyOf(first), ImmutableSet.copyOf(second))
            .stream()
            .collect(toImmutableSet()))
        .filter(Countable::isPopulated)
        .map { it.stream().map(Task::toString).collect(joining(", ")) }
        .map { failurePrefix + it }
        .ifPresent { throw HandlerException(it) }
  }
}