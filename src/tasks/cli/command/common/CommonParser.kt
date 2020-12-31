package tasks.cli.command.common

import org.apache.commons.cli.CommandLine
import tasks.cli.parser.ParserUtil

class CommonParser {
  fun <T> parse(commandLine: CommandLine, specificArguments: T): CommonArguments<T> {
    return CommonArguments(
        specificArguments,
        !ParserUtil.getFlagPresence(commandLine, StripColors.STRIP_COLORS_OPTION.value()))
  }
}