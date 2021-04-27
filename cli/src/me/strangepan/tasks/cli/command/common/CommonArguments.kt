package me.strangepan.tasks.cli.command.common

/**
 * A model representing the command line parameters common to all commands. Instances of this model
 * contain the arguments model specific to the current command.
 *
 * @param <T> the arguments model specific to the current command
</T> */
class CommonArguments<T>(private val specificArguments: T, private val enableColorOutput: Boolean) {

  /** The argument model specific to the current command.  */
  fun specificArguments(): T {
    return specificArguments
  }

  /** True if the output should contain formatting control codes (e.g. color codes).  */
  fun enableOutputFormatting(): Boolean {
    return enableColorOutput
  }
}