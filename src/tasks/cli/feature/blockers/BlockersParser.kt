package tasks.cli.feature.blockers

import java.util.Objects
import omnia.data.cache.Memoized
import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList
import org.apache.commons.cli.CommandLine
import tasks.cli.parser.CommandParser
import tasks.cli.parser.ParseResult
import tasks.cli.parser.Parser
import tasks.cli.parser.ParserException
import tasks.cli.parser.ParserUtil
import tasks.model.Task

/** Command line argument parser for the Blockers command.  */
class BlockersParser(
    taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : CommandParser<BlockersArguments> {
  private val taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>
  override fun parse(commandLine: CommandLine): BlockersArguments {
    /*
     * 1st param assumed to be task ID
     * 2+ params are unsupported
     * optional blockers to add
     * optional blockers to remove
     * optional clear flag
     */
    val argsList: List<String> = ImmutableList.copyOf(commandLine.argList)
    if (argsList.count() < 1) {
      throw ParserException("ObservableTask not specified")
    }
    if (argsList.count() > 1) {
      throw ParserException("Unexpected extra arguments")
    }
    val targetTask = taskParser.value().parse(
        ImmutableList.of<String>(argsList.itemAt(0))).itemAt(0)
    val tasksToAdd = taskParser.value().parse(
        ParserUtil.getOptionValues(commandLine, BlockersCommand.ADD_OPTION.value()))
    val tasksToRemove = taskParser.value().parse(
        ParserUtil.getOptionValues(commandLine, BlockersCommand.REMOVE_OPTION.value()))
    val isClearSet = ParserUtil.getFlagPresence(commandLine, BlockersCommand.CLEAR_OPTION.value())
    ParserUtil.extractSuccessfulResultsOrThrow(
        ImmutableList.builder<ParseResult<*>>()
            .add(targetTask)
            .addAll(tasksToAdd)
            .addAll(tasksToRemove)
            .build())
    return BlockersArguments(
        ParserUtil.extractSuccessfulResultOrThrow(targetTask),
        ParserUtil.extractSuccessfulResultsOrThrow(tasksToAdd),
        ParserUtil.extractSuccessfulResultsOrThrow(tasksToRemove),
        isClearSet)
  }

  init {
    this.taskParser = Objects.requireNonNull(taskParser)
  }
}