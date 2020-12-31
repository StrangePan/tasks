package tasks.model.impl

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.CompletableSubject
import java.util.Optional
import java.util.function.Consumer
import omnia.algorithm.GraphAlgorithms.findAnyCycle
import omnia.data.stream.Collectors.toImmutableList
import omnia.data.structure.DirectedGraph
import omnia.data.structure.Map
import omnia.data.structure.immutable.ImmutableDirectedGraph
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.observable.ObservableState
import omnia.data.structure.observable.ObservableState.Companion.create
import omnia.data.structure.tuple.Couple
import omnia.data.structure.tuple.Triple
import omnia.data.structure.tuple.Tuple
import tasks.io.File
import tasks.model.ObservableTaskStore
import tasks.model.Task
import tasks.model.TaskBuilder
import tasks.model.TaskMutator
import tasks.util.rx.Maybes

/** This is currently NOT thread-safe.  */
class ObservableTaskStoreImpl private constructor(private val taskStorage: TaskStorage) : ObservableTaskStore {
  private val isShutdown = CompletableSubject.create()
  private val isShutdownComplete: Completable
  private val store: ObservableState<TaskStoreImpl>
  private val currentTaskStore: Observable<TaskStoreImpl>

  override fun observe(): Observable<TaskStoreImpl> {
    return currentTaskStore
  }

  override fun createTask(
      label: String, builder: java.util.function.Function<in TaskBuilder, out TaskBuilder>): Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> {
    return Single.just(TaskBuilderImpl(this, label))
        .map(builder::apply)
        .flatMap(::maybeApplyBuilder)
        .cache()
  }

  /**
   * Attempts to apply the given [TaskBuilder] to the task graph, but in a manner that tries
   * to preserve internal consistency by first applying the changes to a snapshot and validating
   * the snapshot.
   */
  private fun maybeApplyBuilder(builder: TaskBuilder): Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> {
    val builderImpl = validateBuilder(builder)
    return store.mutateAndReturn { applyBuilderTo(builderImpl, it) }
        .map { it.second() }
  }

  private fun validateBuilder(builder: TaskBuilder): TaskBuilderImpl {
    require(builder is TaskBuilderImpl) {
      "Unrecognized builder type. Expected ${TaskBuilderImpl::class.java}, received " +
          "${builder.javaClass}: $builder"
    }
    require(builder.store() == this) {
      "Builder associated with another store. Expected <$this>, received <${builder.store()}>: " +
          "$builder"
    }
    return builder
  }

  override fun mutateTask(
      task: Task, mutation: java.util.function.Function<in TaskMutator, out TaskMutator>): Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> {
    return mutateTask(validateTask(task), mutation)
  }

  private fun mutateTask(task: TaskImpl, mutation: java.util.function.Function<in TaskMutator, out TaskMutator>): Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> {
    return Single.just(TaskMutatorImpl(this, task.id()))
        .map { mutation.apply(it) }
        .flatMap(::maybeApplyMutator)
        .cache()
  }

  fun validateTask(task: Task): TaskImpl {
    require(task is TaskImpl) {
      "Unrecognized task type. Expected ${TaskImpl::class.java}, received ${task.javaClass}: $task"
    }
    return task
  }

  private fun maybeApplyMutator(
      mutator: TaskMutator): Single<Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> {
    val mutatorImpl = validateMutator(mutator)
    return store.mutateAndReturn { oldStore ->
      val nextTaskGraph = applyMutatorTo(oldStore.graph, mutatorImpl)
      val nextTaskData = applyMutatorTo(oldStore.data, mutatorImpl)
      assertIsValid(nextTaskGraph, nextTaskData)
      val newStore = TaskStoreImpl(nextTaskGraph, nextTaskData)
      Tuple.of(
          newStore, Tuple.of(oldStore, newStore, newStore.toTask(mutatorImpl.id())))
    }
        .map { it.second() }
  }

  private fun validateMutator(mutator: TaskMutator): TaskMutatorImpl {
    require(mutator is TaskMutatorImpl) {
      "Unrecognized mutator type. Expected ${TaskMutatorImpl::class.java}, received" +
          " ${mutator.javaClass}: $mutator"
    }
    require(mutator.store() == this) {
      "Mutator associated with another store. Expected <$this>, received <${mutator.store()}>: " +
          "$mutator"
    }
    return mutator
  }

  override fun deleteTask(task: Task): Maybe<TaskImpl> {
    return deleteTask(validateTask(task))
  }

  private fun deleteTask(task: TaskImpl): Maybe<TaskImpl> {
    return store.mutateAndReturn { oldTaskStore ->
      val taskToRemove = oldTaskStore.lookUpById(task.id())
      Tuple.of(
          taskToRemove.map {
            TaskStoreImpl(
                oldTaskStore.graph.toBuilder().removeNode(it.id()).build(),
                oldTaskStore.data.toBuilder().removeKey(it.id()).build())
          }
              .orElse(oldTaskStore),
          taskToRemove)
    }
        .map { it.second() }
        .flatMapMaybe(Maybes::fromOptional)
  }

  override fun writeToDisk(): Completable {
    return store.observe()
        .firstOrError()
        .flatMapCompletable { taskStorage.writeToStorage(it.graph, it.data) }
        .cache()
  }

  override fun shutdown(): Completable {
    return isShutdownComplete.doOnSubscribe { isShutdown.onComplete() }
  }

  companion object {
    fun createFromFile(filePath: String): ObservableTaskStoreImpl {
      return ObservableTaskStoreImpl(TaskFileStorage(File.fromPath(filePath)))
    }

    @JvmStatic
    fun createInMemoryStorage(): ObservableTaskStoreImpl {
      return ObservableTaskStoreImpl(
          object : TaskStorage {
            override fun readFromStorage(): Single<Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>>> {
              return Single.just(Tuple.of(ImmutableDirectedGraph.empty(), ImmutableMap.empty()))
            }

            override fun writeToStorage(
                graph: DirectedGraph<out TaskIdImpl>,
                data: Map<out TaskIdImpl, out TaskData>): Completable {
              return Completable.complete()
            }
          })
    }

    private fun applyBuilderTo(builder: TaskBuilderImpl, oldStore: TaskStoreImpl): Couple<TaskStoreImpl, Triple<TaskStoreImpl, TaskStoreImpl, TaskImpl>> {
      val id: TaskIdImpl = TaskIdImpl.generate(oldStore.graph.contents())
      val newGraphBuilder = oldStore.graph.toBuilder().addNode(id)
      builder.blockingTasks().forEach(Consumer { newGraphBuilder.addEdge(it, id) })
      builder.blockedTasks().forEach(Consumer { newGraphBuilder.addEdge(id, it) })
      val newGraph = newGraphBuilder.build()
      val newData = oldStore.data.toBuilder()
          .putMapping(id, TaskData(builder.label(), builder.status()))
          .build()
      assertIsValid(newGraph, newData)
      val newStore = TaskStoreImpl(newGraph, newData)
      return Tuple.of(newStore, Tuple.of(oldStore, newStore, newStore.toTask(id)))
    }

    private fun assertIsValid(
        taskGraph: DirectedGraph<TaskIdImpl>, taskData: Map<TaskIdImpl, TaskData>) {
      findAnyCycle(taskGraph)
          .map { cycle ->
            cycle.stream()
                .map { id ->
                  (id.toString()
                      + ": "
                      + taskData.valueOf(id).map { obj: TaskData -> obj.label() }.orElse(""))
                }
                .collect(toImmutableList())
          }
          .ifPresent { cycle -> throw CyclicalDependencyException("Cycle detected", cycle) }
    }

    private fun applyMutatorTo(
        taskData: ImmutableMap<TaskIdImpl, TaskData>, mutatorImpl: TaskMutatorImpl): ImmutableMap<TaskIdImpl, TaskData> {
      return Optional.of(mutatorImpl)
          .filter { mutator -> mutator.statusMutator().isPresent || mutator.label().isPresent }
          .map { obj -> obj.id() }
          .flatMap { taskData.valueOf(it) }
          .map { data ->
            TaskData(
                mutatorImpl.label().orElse(data.label()),
                mutatorImpl.statusMutator().map { it.apply(data) }.orElse(data.status()))
          }
          .map { data -> taskData.toBuilder().putMapping(mutatorImpl.id(), data).build() }
          .orElse(taskData)
    }

    private fun applyMutatorTo(
        taskGraph: ImmutableDirectedGraph<TaskIdImpl>, mutatorImpl: TaskMutatorImpl): ImmutableDirectedGraph<TaskIdImpl> {
      return Optional.of(mutatorImpl)
          .filter { mutator ->
            (mutator.overwriteBlockedTasks()
                || mutator.blockedTasksToAdd().isPopulated
                || mutator.blockedTasksToRemove().isPopulated
                || mutator.overwriteBlockingTasks()
                || mutator.blockingTasksToAdd().isPopulated
                || mutator.blockingTasksToRemove().isPopulated)
          }
          .map { mutator: TaskMutatorImpl ->
            val id = mutator.id()
            val builder = taskGraph.toBuilder()
            if (mutator.overwriteBlockingTasks()) {
              taskGraph.nodeOf(id)
                  .map(ImmutableDirectedGraph<TaskIdImpl>.DirectedNode::incomingEdges)
                  .map(ImmutableSet.Companion::copyOf)
                  .orElse(ImmutableSet.empty())
                  .forEach { edge -> builder.removeEdge(edge.start().item(), edge.end().item()) }
            }
            mutator.blockingTasksToAdd()
                .forEach(Consumer { builder.addEdge(it, id) })
            mutator.blockingTasksToRemove()
                .forEach(Consumer { builder.removeEdge(it, id) })
            if (mutator.overwriteBlockedTasks()) {
              taskGraph.nodeOf(id)
                  .map { it.outgoingEdges() }
                  .map(ImmutableSet.Companion::copyOf)
                  .orElse(ImmutableSet.empty())
                  .forEach { builder.removeEdge(it.start().item(), it.end().item()) }
            }
            mutator.blockedTasksToAdd().forEach { builder.addEdge(id, it) }
            mutator.blockedTasksToRemove().forEach { builder.removeEdge(id, it) }
            builder.build()
          }
          .orElse(taskGraph)
    }
  }

  init {
    val loadedData = this.taskStorage.readFromStorage().blockingGet()
    store = create(
        TaskStoreImpl(
            loadedData.first(),
            loadedData.second()))
    currentTaskStore = store.observe().takeUntil(isShutdown.andThen(Observable.just(Unit)))
    isShutdownComplete = isShutdown.hide().andThen(writeToDisk()).cache()
  }
}