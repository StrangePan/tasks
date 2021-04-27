package me.strangepan.tasks.cli.feature.info

import java.util.function.Function
import omnia.data.cache.Memoized
import omnia.data.structure.List
import me.strangepan.tasks.cli.command.common.simple.SimpleParser
import me.strangepan.tasks.cli.parser.ParseResult
import me.strangepan.tasks.cli.parser.Parser
import me.strangepan.tasks.engine.model.Task

/** Command line argument parser for the Info command.  */
class InfoParser(
    taskParser: Memoized<out Parser<out List<out ParseResult<out Task>>>>)
    : SimpleParser<InfoArguments>(Function(::InfoArguments), taskParser)