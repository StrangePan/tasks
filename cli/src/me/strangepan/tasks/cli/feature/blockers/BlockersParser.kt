package me.strangepan.tasks.cli.feature.blockers

import omnia.data.cache.Memoized
import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList
import org.apache.commons.cli.CommandLine
import me.strangepan.tasks.cli.parser.CommandParser
import me.strangepan.tasks.cli.parser.ParseResult
import me.strangepan.tasks.cli.parser.Parser
import me.strangepan.tasks.cli.parser.ParserException
import me.strangepan.tasks.cli.parser.ParserUtil
import me.strangepan.tasks.engine.model.Task

/** Command line argument parser for the Blockers command.  */
class BlockersParser(
    private val taskListParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>) : CommandParser<BlockersArguments> {
  override fun parse(commandLine: CommandLine): BlockersArguments {
    /*
     * 1st param assumed to be task ID
     * 2+ params are unsupported
     * optional blockers to add
     * optional blockers to remove
     * optional clear flag
     */
    val argsList = ImmutableList.copyOf(commandLine.argList)
    if (argsList.count() < 1) {
      throw ParserException("ObservableTask not specified")
    }
    val targetTasks = taskListParser.value().parse(argsList)
    val tasksToAdd = taskListParser.value().parse(
        ParserUtil.getOptionValues(commandLine, BlockersCommand.ADD_OPTION.value()))
    val tasksToRemove = taskListParser.value().parse(
        ParserUtil.getOptionValues(commandLine, BlockersCommand.REMOVE_OPTION.value()))
    val isClearSet = ParserUtil.getFlagPresence(commandLine, BlockersCommand.CLEAR_OPTION.value())
    ParserUtil.extractSuccessfulResultsOrThrow(
        ImmutableList.builder<ParseResult<*>>()
            .addAll(targetTasks)
            .addAll(tasksToAdd)
            .addAll(tasksToRemove)
            .build())
    return BlockersArguments(
        ParserUtil.extractSuccessfulResultsOrThrow(targetTasks),
        ParserUtil.extractSuccessfulResultsOrThrow(tasksToAdd),
        ParserUtil.extractSuccessfulResultsOrThrow(tasksToRemove),
        isClearSet)
  }

}