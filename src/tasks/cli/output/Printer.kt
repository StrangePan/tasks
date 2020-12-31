package tasks.cli.output

import java.io.PrintStream
import java.util.Objects
import java.util.function.Function
import java.util.function.Supplier
import omnia.cli.out.Output
import tasks.cli.command.common.CommonArguments

/**
 * A simple abstraction for interfacing with proper Java print streams. Useful for applying uniform
 * formatting options to all CLI output.
 */
class Printer private constructor(private val out: PrintStream, private val renderer: Function<in Output, out String>) {

  /**
   * Concatenates the supplied output to the console and flushes immediately.
   * @param output The output to render and output. Must not be null, but may be empty.
   * @return this printer, allowing method chaining
   */
  fun print(output: Output): Printer {
    out.print(Objects.requireNonNull(renderer.apply(output)))
    return this
  }

  /**
   * Concatenates the supplied output to the console and flushes immediately, and resets the cursor
   * to the next line.
   * @param output The output to render and output. Must not be null, but may be empty.
   * @return this printer, allowing method chaining
   */
  fun printLine(output: Output): Printer {
    out.println(Objects.requireNonNull(renderer.apply(output)))
    return this
  }

  class Factory @JvmOverloads constructor(private val printStreamSupplier: Supplier<out PrintStream> = Supplier { System.out }) {
    fun create(arguments: CommonArguments<*>): Printer {
      return Printer(
          printStreamSupplier.get(),
          if (arguments.enableOutputFormatting()) Function(Output::render) else Function(Output::renderWithoutCodes))
    }
  }

}