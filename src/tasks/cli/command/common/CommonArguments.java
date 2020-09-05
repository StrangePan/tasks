package tasks.cli.command.common;

import static java.util.Objects.requireNonNull;

/**
 * A model representing the command line parameters common to all commands. Instances of this model
 * contain the arguments model specific to the current command.
 *
 * @param <T> the arguments model specific to the current command
 */
public final class CommonArguments<T> {
  private final T specificArguments;
  private final boolean enableColorOutput;

  CommonArguments(T specificArguments, boolean enableColorOutput) {
    this.specificArguments = requireNonNull(specificArguments);
    this.enableColorOutput = enableColorOutput;
  }

  /** The argument model specific to the current command. */
  public T specificArguments() {
    return this.specificArguments;
  }

  /** True if the output should contain formatting control codes (e.g. color codes). */
  public boolean enableOutputFormatting() {
    return enableColorOutput;
  }
}
