package me.strangepan.tasks.engine.model.impl

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Writer
import java.util.Comparator
import java.util.Optional
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream
import omnia.data.cache.Memoized
import omnia.data.cache.Memoized.Companion.memoize
import omnia.data.stream.Collectors.toImmutableMap
import omnia.data.structure.DirectedGraph
import omnia.data.structure.Map
import omnia.data.structure.immutable.ImmutableDirectedGraph
import omnia.data.structure.immutable.ImmutableDirectedGraph.UnknownNodeException
import omnia.data.structure.immutable.ImmutableList
import omnia.data.structure.immutable.ImmutableMap
import omnia.data.structure.immutable.ImmutableMap.Companion.copyOf
import omnia.data.structure.immutable.ImmutableSet
import omnia.data.structure.mutable.HashMap
import omnia.data.structure.mutable.HashSet
import omnia.data.structure.mutable.MutableMap
import omnia.data.structure.mutable.MutableSet
import omnia.data.structure.tuple.Couple
import omnia.data.structure.tuple.Tuple
import me.strangepan.tasks.engine.io.File
import me.strangepan.tasks.engine.model.Task

class TaskFileStorage(private val file: File) : TaskStorage {
  override fun readFromStorage(): Single<Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>>> {
    return Single.fromCallable { BufferedReader(file.openReader()).use { reader -> return@fromCallable parseTaskData(reader) } }
  }

  override fun writeToStorage(
      graph: DirectedGraph<out TaskIdImpl>,
      data: Map<out TaskIdImpl, out TaskData>): Completable {
    return Completable.fromAction { BufferedWriter(file.openWriter()).use { writer -> serializeTaskData(graph, data, writer) } }
  }

  private class LineCollector {
    private val graph: ImmutableDirectedGraph.Builder<TaskIdImpl> = ImmutableDirectedGraph.builder()
    private val tasksWithParsedEdges: MutableSet<TaskIdImpl> = HashSet.create()
    private val tasks: MutableMap<TaskIdImpl, TaskData> = HashMap.create()
    private var state: State = State.PARSING_NOTHING

    private enum class State {
      PARSING_NOTHING, PARSING_GRAPH, PARSING_TASKS
    }

    fun collect(line: String) {
      maybeParseStateChange(line).ifPresentOrElse(
          { newState: State -> state = newState }
      ) { parseToCollection(line) }
    }

    private fun maybeParseStateChange(line: String): Optional<State> {
      val versionMatcher = VERSION_PATTERN.matcher(line)
      if (versionMatcher.matches()) {
        assertSupportedVersion(versionMatcher.group(1))
        return Optional.of(State.PARSING_NOTHING)
      } else if (line == "# tasks") {
        return Optional.of(State.PARSING_TASKS)
      } else if (line == "# dependencies") {
        return Optional.of(State.PARSING_GRAPH)
      }
      return Optional.empty()
    }

    private fun parseToCollection(line: String) {
      when (state) {
        State.PARSING_GRAPH -> parseToGraph(line)
        State.PARSING_TASKS -> parseToTasks(line)
        else -> throw TaskParseError("unexpected line in file: $line")
      }
    }

    private fun parseToGraph(line: String) {
      val fields = line.split(TASK_FIELD_DELIMITER.toRegex()).toTypedArray()
      val id = parseId(fields[0])
      if (tasksWithParsedEdges.contains(id)) {
        throw TaskParseError("edges for task defined twice: $id")
      }
      try {
        Stream.of(*fields[1].split(TASK_ID_DELIMITER.toRegex()).toTypedArray())
            .map { string: String -> parseId(string) }
            .forEach { dependency: TaskIdImpl -> graph.addEdge(dependency, id) }
      } catch (e: UnknownNodeException) {
        throw TaskParseError("missing task data for: $id", e)
      }
      tasksWithParsedEdges.add(id)
    }

    private fun parseToTasks(line: String) {
      val fields = line.split(TASK_FIELD_DELIMITER.toRegex()).toTypedArray()
      val id = parseId(fields[0])
      val status = parseStatus(fields[1])
      val label = unescapeLabel(fields[2])
      if (tasks.keys().contains(id)) {
        throw TaskParseError("task ID defined twice: $id")
      }
      tasks.putMapping(id, TaskData(label, status))
      graph.addNode(id)
    }

    fun build(): Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>> {
      return Tuple.of(graph.build(), copyOf(tasks))
    }

    companion object {
      private fun assertSupportedVersion(versionString: String) {
        try {
          val version = versionString.toInt()
          if (version > VERSION) {
            throw IncompatibleVersionError("unsupported file version: $version. supported versions: $VERSION")
          }
        } catch (ex: NumberFormatException) {
          throw TaskParseError("unable to parse file version code: $versionString", ex)
        }
      }

      private fun parseStatus(field: String): Task.Status {
        return STRING_TO_STATUS.value().valueOf(field.toLowerCase()).orElse(Task.Status.OPEN)
      }
    }
  }

  private fun serializeTaskData(
      graph: DirectedGraph<out TaskIdImpl>,
      data: Map<out TaskIdImpl, out TaskData>,
      writer: Writer) {
    Observable.just(
        Observable.just("# version $VERSION"),
        Observable.just("# tasks"),
        serialize(data),
        Observable.just("# dependencies"),
        serialize(graph))
        .concatMap { o: Observable<String> -> o }
        .concatMap { line: String -> Observable.just(line, END_OF_LINE) }
        .blockingForEach { str: String -> writer.write(str) }
  }

  class IncompatibleVersionError(message: String) : ParseError(message) {

    companion object {
      private const val serialVersionUID = -5818555235686955880L
    }
  }

  class TaskParseError : ParseError {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    companion object {
      private const val serialVersionUID = 7208486740444049242L
    }
  }

  open class ParseError : RuntimeException {
    protected constructor(message: String) : super(message)
    protected constructor(message: String, cause: Throwable) : super(message, cause)

    companion object {
      private const val serialVersionUID = -1135195505560715347L
    }
  }

  companion object {
    private const val VERSION = 2
    private const val END_OF_LINE = "\n"
    private const val TASK_FIELD_DELIMITER = ";"
    private const val TASK_ID_DELIMITER = ","
    private val VERSION_PATTERN = Pattern.compile("^# version (\\d+)$")
    private val STATUS_STRINGS = memoize {
      ImmutableSet.of( // false = !isCompleted, legacy
          Tuple.of(Task.Status.OPEN, ImmutableList.of("open", "false")),  // true = isCompleted, legacy
          Tuple.of(Task.Status.COMPLETED, ImmutableList.of("complete", "true")),
          Tuple.of(Task.Status.STARTED, ImmutableList.of("started")))
    }
    private val STATUS_TO_STRING: Memoized<ImmutableMap<Task.Status, String>> = memoize(Supplier<ImmutableMap<Task.Status, String>> {
      STATUS_STRINGS.value().stream()
          .map { couple: Couple<Task.Status, ImmutableList<String>> -> couple.mapSecond { list: ImmutableList<String> -> list.itemAt(0) } }
          .collect(toImmutableMap())
    })
    private val STRING_TO_STATUS = memoize {
      STATUS_STRINGS.value().stream()
          .flatMap { couple: Couple<Task.Status, ImmutableList<String>> -> couple.second().stream().map { s: String -> Tuple.of(s, couple.first()) } }
          .collect(toImmutableMap())
    }

    private fun parseTaskData(reader: BufferedReader): Couple<ImmutableDirectedGraph<TaskIdImpl>, ImmutableMap<TaskIdImpl, TaskData>> {
      return Single.just(reader)
          .map { obj: BufferedReader -> obj.lines() }
          .map { obj: Stream<String> -> obj.iterator() }
          .map { iterator: Iterator<String> -> Iterable { iterator } }
          .flatMapObservable { source: Iterable<String> -> Observable.fromIterable(source) }
          .filter { line: String -> line.isNotBlank() }
          .collect({ LineCollector() }) { obj: LineCollector, line: String -> obj.collect(line) }
          .map { obj: LineCollector -> obj.build() }
          .blockingGet()
    }

    private fun parseId(string: String): TaskIdImpl {
      return try {
        TaskIdImpl.parse(string)
      } catch (ex: NumberFormatException) {
        throw TaskParseError("invalid id: $string", ex)
      }
    }

    private fun serialize(tasks: Map<out TaskIdImpl, out TaskData>): Observable<String> {
      return Single.just(tasks)
          .map { obj -> obj.entries() }
          .flatMapObservable { source -> Observable.fromIterable(source) }
          .sorted(Comparator.comparing { entry -> entry.key().asLong() })
          .map { entry -> Tuple.of(entry.key(), entry.value()) }
          .map { task -> serialize(task) }
    }

    private fun serialize(task: Couple<out TaskIdImpl, out TaskData>): String {
      return StringBuilder()
          .append(serialize(task.first()))
          .append(TASK_FIELD_DELIMITER)
          .append(STATUS_TO_STRING.value().valueOf(task.second().status()).orElseThrow())
          .append(TASK_FIELD_DELIMITER)
          .append(escapeLabel(task.second().label()))
          .append(TASK_FIELD_DELIMITER)
          .toString()
    }

    private fun serialize(id: TaskIdImpl): String {
      return id.toString()
    }

    private fun escapeLabel(label: String): String {
      val escapedLabel = StringBuilder()
      var i = 0
      for (j in 0..label.length) {
        val isEscapableCharacter = j < label.length && isEscapableCharacter(label[j])
        if (j == label.length || isEscapableCharacter) {
          escapedLabel.append(label, i, j)
          i = j + 1
        }
        if (j < label.length && isEscapableCharacter) {
          escapedLabel.append("\\").append(escapeCharacter(label[j]))
        }
      }
      return escapedLabel.toString()
    }

    private fun isEscapableCharacter(c: Char): Boolean {
      return when (c) {
        '\\', '\n', ';' -> true
        else -> false
      }
    }

    private fun escapeCharacter(c: Char): Char {
      return when (c) {
        '\\' -> '\\'
        '\n' -> 'n'
        ';' -> ':'
        else -> c
      }
    }

    private fun unescapeLabel(escapedLabel: String): String {
      val label = StringBuilder()
      var i = 0
      var j = 0
      while (j <= escapedLabel.length) {
        val isEscapeCharacter = j < escapedLabel.length && escapedLabel[j] == '\\'
        if (j == escapedLabel.length || isEscapeCharacter) {
          label.append(escapedLabel, i, j)
          i = j + 2
        }
        if (j < escapedLabel.length && isEscapeCharacter) {
          j++
          label.append(unescapeCharacter(escapedLabel[j]))
        }
        j++
      }
      return label.toString()
    }

    private fun unescapeCharacter(c: Char): Char {
      return when (c) {
        '\\' -> '\\'
        'n' -> '\n'
        ':' -> ';'
        else -> c
      }
    }

    private fun serialize(graph: DirectedGraph<out TaskIdImpl>): Observable<String> {
      return Single.just(graph)
          .map { obj -> obj.nodes() }
          .flatMapObservable { source -> Observable.fromIterable(source) }
          .filter { node -> node.predecessors().isPopulated }
          .sorted(Comparator.comparing { node -> node.item().asLong() })
          .map { node -> serialize(node) }
    }

    private fun serialize(node: DirectedGraph.DirectedNode<out TaskIdImpl>): String {
      return StringBuilder()
          .append(serialize(node.item()))
          .append(TASK_FIELD_DELIMITER)
          .append(
              node.predecessors().stream()
                  .map(DirectedGraph.DirectedNode<out TaskIdImpl>::item)
                  .sorted(Comparator.comparing { obj -> obj.asLong() })
                  .map { id -> serialize(id) }
                  .collect(Collectors.joining(TASK_ID_DELIMITER)))
          .append(TASK_FIELD_DELIMITER)
          .toString()
    }
  }
}