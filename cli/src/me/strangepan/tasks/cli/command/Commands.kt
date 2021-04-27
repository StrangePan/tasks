package me.strangepan.tasks.cli.command

import java.util.Optional
import omnia.data.structure.Collection

interface Commands {
  /** Gets all registered commands.  */
  val allCommands: Collection<Command>

  /**
   * Finds and returns the only command that matches the provided string (presumed to be
   * [userInput]), or [Optional.empty] if there are no matches. Compares command canonical names and
   * aliases.
   */
  fun getMatchingCommand(userInput: String): Optional<Command>
}