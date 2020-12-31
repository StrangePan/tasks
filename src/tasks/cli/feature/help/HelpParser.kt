package tasks.cli.feature.help

import java.util.Optional
import omnia.data.structure.List
import omnia.data.structure.immutable.ImmutableList
import org.apache.commons.cli.CommandLine
import tasks.cli.parser.CommandParser

/** Command line argument parser for the Help command.  */
class HelpParser : CommandParser<HelpArguments> {
  override fun parse(commandLine: CommandLine): HelpArguments {
    val parsedArgs: List<String> = ImmutableList.copyOf(commandLine.argList)
    val mode = if (parsedArgs.count() > 0) Optional.of(parsedArgs.itemAt(0)) else Optional.empty()
    return mode.map(::HelpArguments).orElseGet(::HelpArguments)
  }
}