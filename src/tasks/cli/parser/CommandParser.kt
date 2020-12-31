package tasks.cli.parser

import org.apache.commons.cli.CommandLine

interface CommandParser<T : Any> {
  fun parse(commandLine: CommandLine): T
}