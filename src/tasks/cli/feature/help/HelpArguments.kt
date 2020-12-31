package tasks.cli.feature.help

import java.util.Objects
import java.util.Optional

/** Model for parsed Help command arguments.  */
class HelpArguments private constructor(mode: Optional<String>) {
  private val mode: Optional<String>

  internal constructor() : this(Optional.empty<String>())
  internal constructor(mode: String) : this(Optional.of<String>(mode))

  /** The optional command for which the user is requesting help.  */
  fun mode(): Optional<String> {
    return mode
  }

  init {
    this.mode = Objects.requireNonNull(mode)
  }
}