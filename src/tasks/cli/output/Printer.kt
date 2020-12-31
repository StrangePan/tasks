package tasks.cli.output;

import static java.util.Objects.requireNonNull;

import java.io.PrintStream;
import java.util.function.Function;
import java.util.function.Supplier;
import omnia.cli.out.Output;
import tasks.cli.command.common.CommonArguments;

/**
 * A simple abstraction for interfacing with proper Java print streams. Useful for applying uniform
 * formatting options to all CLI output.
 */
public final class Printer {

  private final PrintStream out;
  private final Function<? super Output, ? extends String> renderer;

  private Printer(PrintStream out, Function<? super Output, ? extends String> renderer) {
    this.out = requireNonNull(out);
    this.renderer = requireNonNull(renderer);
  }

  /**
   * Concatenates the supplied output to the console and flushes immediately.
   * @param output The output to render and output. Must not be null, but may be empty.
   * @return this printer, allowing method chaining
   */
  public Printer print(Output output) {
    out.print(requireNonNull(renderer.apply(output)));
    return this;
  }

  /**
   * Concatenates the supplied output to the console and flushes immediately, and resets the cursor
   * to the next line.
   * @param output The output to render and output. Must not be null, but may be empty.
   * @return this printer, allowing method chaining
   */
  public Printer printLine(Output output) {
    out.println(requireNonNull(renderer.apply(output)));
    return this;
  }

  public static final class Factory {
    private final Supplier<? extends PrintStream> printStreamSupplier;

    public Factory() {
      this(() -> System.out);
    }

    public Factory(Supplier<? extends PrintStream> printStreamSupplier) {
      this.printStreamSupplier = printStreamSupplier;
    }

    public Printer create(CommonArguments<?> arguments) {
      return new Printer(
          printStreamSupplier.get(),
          arguments.enableOutputFormatting() ? Output::render : Output::renderWithoutCodes);
    }
  }
}
