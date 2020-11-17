package tasks.cli.output;

import static java.util.Objects.requireNonNull;

import java.io.PrintStream;
import java.util.function.Function;
import omnia.cli.out.Output;
import tasks.cli.command.common.CommonArguments;

public final class Printer {

  private final PrintStream out;
  private final Function<? super Output, ? extends String> renderer;

  private Printer(PrintStream out, Function<? super Output, ? extends String> renderer) {
    this.out = requireNonNull(out);
    this.renderer = requireNonNull(renderer);
  }

  public Printer print(Output output) {
    out.print(requireNonNull(renderer.apply(output)));
    return this;
  }

  public Printer printLine(Output output) {
    out.println(requireNonNull(renderer.apply(output)));
    return this;
  }

  public static final class Factory {
    public Printer create(CommonArguments<?> arguments) {
      return new Printer(
          System.out,
          arguments.enableOutputFormatting() ? Output::render : Output::renderWithoutCodes);
    }
  }
}
