package tasks.cli.parser

import omnia.data.structure.List

interface Parser<T : Any> {
  fun parse(commandLine: List<out String>): T
}